

#define MAXTOPIC 1024

int drawFromProportionalMultinomial(const float* prob, float rand) {
    int i = 0;
    while (rand > prob[i]) {
        rand -= prob[i];
        i++;
    }
    return i;
}

float basicGaussian(float x) {
    float gaussDenominator = 2.5066282; /* sqrt( 2 PI ) = 2.5066282746310005024157652848110452530069867406099383 */
    return exp(-x * x / 2) / gaussDenominator;
}
float gaussian(float x, float mu, float stddev) {
    return basicGaussian((x - mu) / stddev) / stddev;
}

__kernel void compute_updates(
    __global const float* obs,
    __global const float* stats,
    __global const int* z,
    __global int* updates,
    __global float* rand,
    __constant const float* fixedSigmaDiag,
    __constant const float* hMu0,
    __constant const float* hSigma0Diag,
    int dimension,
    float alpha,
    const int componentCount,
    int from,
    int limit)
{
    int i = get_global_id(0);
    int iObs = from + i;
    if (iObs >= limit) return;

    int oldZ = z[iObs];
    
    private float p[MAXTOPIC];
    float sum = 0;
    for (int k = 0; k < MAXTOPIC; k++) p[k] = 0;

    for (int k = 0; k < componentCount; k++) {
        int statsOffset = k*(1 + 2 * dimension);
        if (k == oldZ) {
            float nObsOfK = stats[statsOffset+0] - 1;
            p[k] = nObsOfK;
            for (int c = 0; c < dimension; c++) {
                float x = obs[iObs*dimension + c];
                float sumC = stats[statsOffset+1+c] - x;
                //
                float sigma0Prime = 1. / (1. / hSigma0Diag[c] + nObsOfK * 1. / fixedSigmaDiag[c]);
                float mu0Prime = sigma0Prime * (1. / hSigma0Diag[c] * hMu0[c] + nObsOfK * 1. / fixedSigmaDiag[c] * sumC / nObsOfK);
                // 
                float mu = mu0Prime;
                float sigma = sigma0Prime + fixedSigmaDiag[c];
                p[k] *= gaussian(x, mu, sqrt(sigma));
            }
        } else {
            float nObsOfK = stats[statsOffset+0];
            p[k] = nObsOfK;
            for (int c = 0; c < dimension; c++) {
                float x = obs[iObs*dimension + c];
                float sumC = stats[statsOffset+1+c];
                //
                float sigma0Prime = 1. / (1. / hSigma0Diag[c] + nObsOfK * 1. / fixedSigmaDiag[c]);
                float mu0Prime = sigma0Prime * (1. / hSigma0Diag[c] * hMu0[c] + nObsOfK * 1. / fixedSigmaDiag[c] * sumC / nObsOfK);
                // 
                float mu = mu0Prime;
                float sigma = sigma0Prime + fixedSigmaDiag[c];
                p[k] *= gaussian(x, mu, sqrt(sigma));
            }
        }
        sum += p[k];
    }
    {
        p[componentCount] = alpha;
        for (int c = 0; c < dimension; c++) {
            float x = obs[iObs*dimension + c];
            float mu = hMu0[c];
            float sigma = hSigma0Diag[c] + fixedSigmaDiag[c];
            p[componentCount] *= gaussian(x, mu, sqrt(sigma));
        }
        sum += p[componentCount]*0.99f;
    }
    int newZ = drawFromProportionalMultinomial(p, rand[i]*sum);
    updates[3*i+0] = iObs;
    updates[3*i+1] = oldZ;
    updates[3*i+2] = newZ;
}

__kernel void apply_updates(
    __global const float* obs,
    __global const int* updates,
    __global float* stats,
    __global int* z,
    int limit,
    int nUpdates,
    int dimension)
{
    int k = get_global_id(0);
    if (k > limit) return; // NB: k==limit means "new component"
    int statsOffset = k*(1 + 2 * dimension);
    int nextComponent = limit;
    
    // TODO: try perf with this for loop in each of the if cases below
    for (int i = 0; i < nUpdates; i++) {
        int oldZ = updates[3*i+1];
        int newZ = updates[3*i+2];
        int iObs = updates[3*i+0];
        if (k == limit) { // new component(s)
            if (newZ == k) {
                z[iObs] = nextComponent;
                const int nstatsOffset = nextComponent*(1 + 2 * dimension);
                stats[nstatsOffset+0] = 1;
                for (int c = 0; c < dimension; c++) {
                    float obsOfC = obs[iObs*dimension + c];
                    stats[nstatsOffset+1+c] = obsOfC;
                    stats[nstatsOffset+1+dimension+c] = obsOfC*obsOfC;
                }                
                nextComponent += 1;
            }
        } else if (oldZ != newZ) { // existing component
            if (oldZ == k) {
                stats[statsOffset+0] -= 1;
                for (int c = 0; c < dimension; c++) {
                    float obsOfC = obs[iObs*dimension + c];
                    stats[statsOffset+1+c] -= obsOfC;
                    stats[statsOffset+1+dimension+c] -= obsOfC*obsOfC;
                }
            }
            if (newZ == k) {
                z[iObs] = newZ;
                stats[statsOffset+0] += 1;
                for (int c = 0; c < dimension; c++) {
                    float obsOfC = obs[iObs*dimension + c];
                    stats[statsOffset+1+c] += obsOfC;
                    stats[statsOffset+1+dimension+c] += obsOfC*obsOfC;
                }
            }
        }
    }
}
