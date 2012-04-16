
#pragma OPENCL EXTENSION cl_khr_fp64 : enable
__kernel void add_doubles(__global const double* a, __global const double* b, __global double* out, int n) 
{
    int i = get_global_id(0);
    if (i >= n)
        return;

    out[i] = a[i] + b[i];
}

__kernel void fill_in_doubles(__global double* a, __global double* b, int n) 
{
    int i = get_global_id(0);
    if (i >= n)
        return;

    a[i] = cos((double)i);
    b[i] = sin((double)i);
}
