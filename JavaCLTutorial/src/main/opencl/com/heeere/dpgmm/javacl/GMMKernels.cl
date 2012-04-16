
__kernel void compute_updates(
    __global const float* obs,
    __global const float* stats,
    __global const int* z,
    __global int* updates,
    int dim,
    float alpha,
    int limit)
{
    int i = get_global_id(0);
    if (i >= limit) return;
    
    int oldZ = z[i];
    int newZ = 0;
    
    updates[i+0] = i;
    updates[i+1] = oldZ;
    updates[i+2] = newZ;
}

__kernel void apply_updtase(
    __global const float* obs,
    __global const int* updates,
    __global float* stats,
    __global int* z,
    int limit)
{
    int k = get_global_id(0);
    if (k >= limit) return;

}