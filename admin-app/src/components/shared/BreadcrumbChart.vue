<script setup lang="ts">
import { ref, computed } from 'vue';

/*Chart*/
const chartOptions = computed(() => {
    return {
        series: [
            {
                name: '',
                data: [5, 8, 7, 12, 6, 7, 15, 20]
            }
        ],
        chart: {
            type: 'bar',
            width: 70,
            height: 40,
            toolbar: {
                show: false
            },
            sparkline: {
                enabled: true
            }
        },
        colors: ['rgba(var(--v-theme-primary))'],
        grid: {
            show: false
        },
        plotOptions: {
            bar: {
                horizontal: false,
                borderRadius: 2,
                columnWidth: '50%',
                barHeight: '100%'
            }
        },
        dataLabels: {
            enabled: false
        },
        stroke: {
            show: true,
            width: 0,
            colors: ['transparent']
        },
        xaxis: {
            axisBorder: {
                show: false
            },
            axisTicks: {
                show: false
            },
            labels: {
                show: false
            }
        },
        yaxis: {
            labels: {
                show: false
            }
        },
        axisBorder: {
            show: false
        },
        fill: {
            opacity: 1
        },
        tooltip: {
            theme: 'dark',
            style: {
                fontFamily: 'inherit'
            },
            x: {
                show: false
            },
            y: {
                formatter: undefined
            }
        }
    };
});

const chartOptions2 = computed(() => {
    return {
        series: [
            {
                name: '',
                data: [5, 8, 7, 12, 6, 7, 15, 20]
            }
        ],
        chart: {
            type: 'bar',
            width: 70,
            height: 40,
            toolbar: {
                show: false
            },
            sparkline: {
                enabled: true
            }
        },
        colors: ['rgba(var(--v-theme-secondary))'],
        grid: {
            show: false
        },
        plotOptions: {
            bar: {
                horizontal: false,
                borderRadius: 2,
                columnWidth: '50%',
                barHeight: '100%'
            }
        },
        dataLabels: {
            enabled: false
        },
        stroke: {
            show: true,
            width: 0,
            colors: ['transparent']
        },
        xaxis: {
            axisBorder: {
                show: false
            },
            axisTicks: {
                show: false
            },
            labels: {
                show: false
            }
        },
        yaxis: {
            labels: {
                show: false
            }
        },
        axisBorder: {
            show: false
        },
        fill: {
            opacity: 1
        },
        tooltip: {
            theme: 'dark',
            style: {
                fontFamily: 'inherit'
            },
            x: {
                show: false
            },
            y: {
                formatter: undefined
            }
        }
    };
});
</script>
<template>
    <div class="d-sm-flex d-none ga-4 justify-content-end align-center">
        <div class="d-flex ga-2">
            <div>
                <p class="textSecondary text-13 mb-0 lh-normal">This Month</p>
                <h5 class="text-h5 text-primary mb-0 font-weight-medium mt-n1">$58,256</h5>
            </div>
            <div>
                <apexchart type="bar" height="40" width="70" :options="chartOptions" :series="chartOptions.series"></apexchart>
            </div>
        </div>
        <div class="d-flex ga-2">
            <div>
                <p class="textSecondary text-13">last Month</p>
                <h5 class="text-h5 text-secondary mb-0 font-weight-medium mt-n1">$58,256</h5>
            </div>
            <div>
                <apexchart type="bar" height="40" width="70" :options="chartOptions2" :series="chartOptions2.series"></apexchart>
            </div>
        </div>
    </div>
</template>
