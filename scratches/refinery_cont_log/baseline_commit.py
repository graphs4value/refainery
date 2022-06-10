import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.036, 0.037, 11.22, 11.041, 0.003, 0.003, 0.073, 0.072, 24.227, 22.797, 0.004, 0.004, 0.126, 0.126, 41.816, 47.581, 0.009, 0.008, 0.237, 0.235, 76.598, 70.29, 0.017, 0.017, 0.471, 0.465, 175.322, 172.678, 0.035, 0.041, 1.348, 0.95, 302.487, 299.377, 0.067, 0.067, 1.895, 1.843, 624.762, 650.508, 0.13, 0.129, 3.624, 3.516, 1424.046, 1432.195, 0.26, 0.262, 7.471, 7.406, 2778.98, 2714.829, 0.516, 0.522, 14.239, 14.217, 0.992, 0.974, 57.801, 55.976]
data_get_all = [0.005316801, 0.005105028, 0.18417684099999998, 0.18074481599999997, 24.158496960999997, 24.061479474, 0.010292842, 0.010227515999999999, 0.363387226, 0.309154079, 49.286199736, 46.261493617999996, 0.020058049999999997, 0.019825699, 0.70936679, 0.705398265, 95.711475455, 96.66383969699999, 0.039659243999999996, 0.039621239, 1.415842324, 1.40053434, 180.08453666699998, 182.13235666699998, 0.07698983699999999, 0.075396285, 2.8776907489999997, 2.484480954, 384.282713333, 373.58635999999996, 0.177768222, 0.178984283, 5.612889143, 5.620132386, 742.52366, 709.25771, 0.356567146, 0.35679159499999996, 11.110992596, 11.304551183, 1504.07812, 1424.02398, 0.713262102, 0.7049974529999999, 22.283904152999998, 22.270737449, 3232.0364799999998, 3205.5303799999997, 1.4421407979999998, 1.407405713, 44.70532260899999, 45.604168379, 7076.70288, 7193.8849199999995, 2.8538303519999997, 2.853230499, 90.144211667, 91.578002727, 5.700673457, 5.770768642, 180.09706666699998, 181.44208999999998]
data_put = [0.003, 0.003, 0.006, 0.006, 0.012, 0.013, 0.02, 0.025, 0.047, 0.058, 0.093, 0.108, 0.2, 0.238, 0.37, 0.445, 0.884, 0.892, 1.695, 1.793, 0.002, 0.002, 0.007, 0.007, 0.016, 0.017, 0.06, 0.074, 0.193, 0.229, 0.739, 0.867, 1.87, 2.366, 4.397, 6.602, 8.386, 9.569, 16.638, 16.696, 0.003, 0.003, 0.006, 0.006, 0.013, 0.013, 0.007, 0.006, 0.004, 0.008, 0.011, 0.008, 0.063, 0.087, 0.152, 0.285, 3.756, 0.779, 240.421, 308.455]
data_put_all = [0.001, 0.001, 0.002, 0.002, 0.003, 0.003, 0.005, 0.005, 0.009, 0.01, 0.019, 0.018, 0.037, 0.036, 0.096, 0.095, 0.19, 0.185, 0.283, 0.374, 0.713, 0.712, 1.456, 1.381, 2.861, 2.96, 5.663, 6.035, 12.263, 12.163, 0.048, 0.049, 0.064, 0.067, 0.098, 0.101, 0.177, 0.179, 0.355, 0.351, 0.706, 0.693, 1.376, 1.349, 2.691, 2.653, 5.443, 5.427, 10.619, 10.771, 22.47, 22.335, 44.869, 44.815, 94.066, 88.821, 181.748, 187.579, 404.9, 456.136, 13.637, 13.893, 19.273, 16.134, 25.758, 25.852, 41.647, 43.839, 92.471, 88.588, 137.824, 141.177, 269.849, 257.635, 557.789, 514.069, 1197.668, 1171.632, 2425.701, 2330.3, 4857.907, 4938.263, 10402.365, 10075.787, 14683.055, 14738.022]
data_getDiffCursor = [0.033, 0.028, 0.871, 0.847, 138.883, 162.75, 0.057, 0.059, 1.69, 1.737, 276.074, 274.541, 0.117, 0.117, 3.406, 3.253, 590.591, 576.551, 0.226, 0.229, 6.513, 6.856, 1358.317, 1234.855, 0.454, 0.482, 12.889, 13.909, 2556.91, 2416.141, 0.909, 0.925, 27.009, 27.543, 5078.693, 4941.817, 1.845, 1.958, 53.425, 55.631, 10119.94, 9466.928, 3.785, 3.774, 113.033, 108.274, 20353.776, 19920.819, 7.666, 7.65, 234.262, 238.578, 39480.987, 38612.571, 14.19, 15.247, 492.595, 532.174, 80501.261, 75351.543, 29.713, 31.694, 1060.882, 1057.992, 161121.43, 166342.781, 0.028, 0.027, 0.885, 0.839, 143.598, 140.947, 0.056, 0.059, 1.729, 1.653, 304.337, 286.374, 0.116, 0.115, 4.006, 4.448, 745.184, 734.46, 0.272, 0.293, 8.357, 7.745, 1654.263, 1405.392, 0.618, 0.665, 19.294, 16.144, 3233.448, 3147.276, 1.033, 0.992, 33.612, 34.977, 8515.544, 7965.831, 1.969, 1.926, 56.857, 60.123, 12152.476, 13088.074, 3.708, 3.937, 113.605, 112.001, 27223.207, 29652.92, 7.327, 8.036, 231.644, 239.674, 62297.065, 63567.856, 17.296, 17.339, 595.246, 706.167, 110198.189, 98737.288, 33.85, 32.171, 1087.237, 1118.576, 235396.217, 205714.364]
data_commit = [121.781, 0.979, 1.045, 294.933, 382.167, 595.707, 538.445, 796.784, 907.781, 1794.963, 2062.859, 0.334, 0.317, 10.37, 4929.837, 4080.602, 0.01, 0.01, 1616.755, 1704.386, 0.397, 11.359, 10.591, 4578.035, 3689.052, 0.638, 0.587, 17.922, 19.408, 40.379, 4.282, 3.322, 113.227, 5.688, 5.629, 343.622, 355.405, 11.238, 1372.257, 1176.905]
data_restore = [0.001, 0.001, 0.002, 0.003, 0.006, 0.005, 0.01, 0.01, 0.021, 0.019, 0.053, 0.046, 0.092, 0.106, 0.001, 0.001, 0.002, 0.003, 0.005, 0.005, 0.01, 0.011, 0.023, 0.021, 0.041, 0.046, 0.095, 0.087, 0.025, 0.003, 0.002, 0.003, 0.006, 0.005, 0.013, 0.022, 0.023, 0.023, 0.09, 0.071, 0.15, 0.127, 0.001, 0.001, 0.002, 0.003, 0.005, 0.005, 0.01, 0.01, 0.022, 0.02, 0.047, 0.041, 0.091, 0.136, 0.002, 0.001, 0.003, 0.002, 0.005, 0.005, 0.012, 0.012, 0.026, 0.046, 0.057, 0.052, 0.136, 0.085, 0.001, 0.004, 0.009, 0.003, 0.029, 0.026, 0.025]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking continuous version',
    xlabel='Baseline methods with commit ',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
