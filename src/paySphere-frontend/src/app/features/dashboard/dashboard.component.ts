import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin } from 'rxjs';
import { DashboardService } from '../../core/services/dashboard.service';
import {
  CountrySalaryResponse,
  DashboardSummaryResponse,
  DepartmentCountResponse,
  DepartmentSalaryResponse,
  SalaryDistributionResponse,
  TopPaidEmployeeResponse
} from '../../core/models/dashboard.model';
import { MoneyPipe } from '../../shared/pipes/money.pipe';

const CHART_PALETTE = ['#3f51b5', '#00acc1', '#ff7043', '#66bb6a', '#ab47bc', '#ffca28', '#26a69a', '#ec407a', '#8d6e63', '#5c6bc0'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
    MoneyPipe
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  readonly loading = signal(true);

  readonly summary = signal<DashboardSummaryResponse | null>(null);
  readonly topPaid = signal<TopPaidEmployeeResponse[]>([]);
  readonly distributions = signal<SalaryDistributionResponse[]>([]);
  readonly selectedCurrency = signal<string | null>(null);

  readonly topPaidColumns = ['employeeCode', 'fullName', 'departmentName', 'countryName', 'totalCompensation'];

  readonly currencyOptions = computed(() => this.distributions().map((d) => d.currencyCode));

  readonly departmentSalaryChart = signal<ChartConfiguration<'bar'>['data']>({ labels: [], datasets: [] });
  readonly countrySalaryChart = signal<ChartConfiguration<'bar'>['data']>({ labels: [], datasets: [] });
  readonly departmentCountChart = signal<ChartConfiguration<'doughnut'>['data']>({ labels: [], datasets: [] });
  readonly distributionChart = signal<ChartConfiguration<'bar'>['data']>({ labels: [], datasets: [] });

  readonly barOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } }
  };

  readonly doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right' } }
  };

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadAll();
  }

  onCurrencyChange(currency: string): void {
    this.selectedCurrency.set(currency);
    this.updateDistributionChart();
  }

  private loadAll(): void {
    this.loading.set(true);

    forkJoin({
      summary: this.dashboardService.getSummary(),
      byDepartment: this.dashboardService.getSalaryByDepartment(),
      byCountry: this.dashboardService.getSalaryByCountry(),
      countByDepartment: this.dashboardService.getEmployeeCountByDepartment(),
      distribution: this.dashboardService.getSalaryDistribution(),
      topPaid: this.dashboardService.getTopPaidEmployees(10)
    }).subscribe({
      next: ({ summary, byDepartment, byCountry, countByDepartment, distribution, topPaid }) => {
        this.summary.set(summary);
        this.topPaid.set(topPaid);
        this.distributions.set(distribution);

        const defaultCurrency =
          distribution.find((d) => d.currencyCode === summary.primaryCurrencyCode)?.currencyCode ??
          distribution[0]?.currencyCode ??
          null;
        this.selectedCurrency.set(defaultCurrency);

        this.buildDepartmentSalaryChart(byDepartment);
        this.buildCountrySalaryChart(byCountry);
        this.buildDepartmentCountChart(countByDepartment);
        this.updateDistributionChart();

        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private buildDepartmentSalaryChart(data: DepartmentSalaryResponse[]): void {
    this.departmentSalaryChart.set({
      labels: data.map((d) => d.departmentName),
      datasets: [
        {
          label: 'Avg. total compensation',
          data: data.map((d) => d.averageTotalCompensation),
          backgroundColor: '#3f51b5',
          borderRadius: 6,
          maxBarThickness: 36
        }
      ]
    });
  }

  private buildCountrySalaryChart(data: CountrySalaryResponse[]): void {
    this.countrySalaryChart.set({
      labels: data.map((d) => d.countryName),
      datasets: [
        {
          label: 'Avg. total compensation',
          data: data.map((d) => d.averageTotalCompensation),
          backgroundColor: '#00acc1',
          borderRadius: 6,
          maxBarThickness: 36
        }
      ]
    });
  }

  private buildDepartmentCountChart(data: DepartmentCountResponse[]): void {
    this.departmentCountChart.set({
      labels: data.map((d) => d.departmentName),
      datasets: [
        {
          data: data.map((d) => d.employeeCount),
          backgroundColor: CHART_PALETTE
        }
      ]
    });
  }

  private updateDistributionChart(): void {
    const current = this.distributions().find((d) => d.currencyCode === this.selectedCurrency());
    if (!current) {
      this.distributionChart.set({ labels: [], datasets: [] });
      return;
    }

    this.distributionChart.set({
      labels: current.buckets.map((b) => `${this.formatCompact(b.rangeStart)} - ${this.formatCompact(b.rangeEnd)}`),
      datasets: [
        {
          label: `Employees (${current.currencyCode})`,
          data: current.buckets.map((b) => b.employeeCount),
          backgroundColor: '#ff7043',
          borderRadius: 6,
          maxBarThickness: 36
        }
      ]
    });
  }

  private formatCompact(value: number): string {
    return new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(value);
  }
}
