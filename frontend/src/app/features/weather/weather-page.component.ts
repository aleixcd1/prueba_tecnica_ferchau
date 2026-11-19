import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, distinctUntilChanged, map, of, switchMap, timer } from 'rxjs';
import { Municipality } from '../../core/models/municipality';
import { TemperatureUnit, WeatherForecast } from '../../core/models/weather-forecast';
import { WeatherApiService } from '../../core/services/weather-api.service';

interface SearchResult {
  municipalities: Municipality[] | null;
  error: string | null;
}

interface ForecastResult {
  forecast: WeatherForecast | null;
  error: string | null;
}

interface ForecastRequest {
  municipalityId: string;
  unit: TemperatureUnit;
}

@Component({
  selector: 'app-weather-page',
  templateUrl: './weather-page.component.html',
  styleUrl: './weather-page.component.css',
})
export class WeatherPageComponent {
  readonly query = signal('');
  readonly suggestions = signal<Municipality[]>([]);
  readonly selectedMunicipality = signal<Municipality | null>(null);
  readonly selectedUnit = signal<TemperatureUnit | ''>('');
  readonly forecast = signal<WeatherForecast | null>(null);
  readonly activeOptionIndex = signal(0);
  readonly searching = signal(false);
  readonly loadingForecast = signal(false);
  readonly searchError = signal('');
  readonly forecastError = signal('');

  private readonly weatherApi = inject(WeatherApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchRequests = new Subject<string>();
  private readonly forecastRequests = new Subject<ForecastRequest | null>();

  constructor() {
    this.searchRequests
      .pipe(
        map((query) => query.trim()),
        distinctUntilChanged(),
        switchMap((query) => {
          if (query.length < 2) {
            return of<SearchResult>({ municipalities: [], error: null });
          }
          this.searching.set(true);
          this.searchError.set('');
          return timer(250).pipe(
            switchMap(() => this.weatherApi.searchMunicipalities(query)),
            map((municipalities): SearchResult => ({ municipalities, error: null })),
            catchError((error: unknown) =>
              of<SearchResult>({ municipalities: null, error: this.errorMessage(error) }),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ municipalities, error }) => {
        this.searching.set(false);
        this.suggestions.set(municipalities ?? []);
        this.activeOptionIndex.set(0);
        this.searchError.set(error ?? '');
      });

    this.forecastRequests
      .pipe(
        distinctUntilChanged(
          (previous, current) =>
            previous?.municipalityId === current?.municipalityId &&
            previous?.unit === current?.unit,
        ),
        switchMap((request) => {
          if (!request) {
            this.loadingForecast.set(false);
            return of<ForecastResult>({ forecast: null, error: null });
          }
          this.loadingForecast.set(true);
          this.forecastError.set('');
          return this.weatherApi.getNextDayForecast(request.municipalityId, request.unit).pipe(
            map((forecast): ForecastResult => ({ forecast, error: null })),
            catchError((error: unknown) =>
              of<ForecastResult>({ forecast: null, error: this.errorMessage(error) }),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ forecast, error }) => {
        this.loadingForecast.set(false);
        this.forecast.set(forecast);
        this.forecastError.set(error ?? '');
      });
  }

  onMunicipalityInput(value: string): void {
    this.query.set(value);
    if (this.selectedMunicipality()?.nombre !== value.trim()) {
      this.selectedMunicipality.set(null);
      this.forecast.set(null);
      this.forecastError.set('');
      this.forecastRequests.next(null);
    }
    this.searchError.set('');
    this.searchRequests.next(value);
  }

  selectMunicipality(municipality: Municipality): void {
    this.selectedMunicipality.set(municipality);
    this.query.set(municipality.nombre);
    this.suggestions.set([]);
    this.searchRequests.next('');
    this.loadForecast();
  }

  onUnitChange(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement)) {
      return;
    }
    const value = event.target.value;
    if (value === '' || value === 'G_CEL' || value === 'G_FAH') {
      this.selectedUnit.set(value);
      this.loadForecast();
    }
  }

  onMunicipalityKeydown(event: KeyboardEvent): void {
    const lastIndex = this.suggestions().length - 1;
    if (event.key === 'ArrowDown' && lastIndex >= 0) {
      event.preventDefault();
      this.activeOptionIndex.update((index) => Math.min(index + 1, lastIndex));
    } else if (event.key === 'ArrowUp' && lastIndex >= 0) {
      event.preventDefault();
      this.activeOptionIndex.update((index) => Math.max(index - 1, 0));
    } else if (event.key === 'Enter' && this.suggestions().length > 0) {
      event.preventDefault();
      this.selectMunicipality(this.suggestions()[this.activeOptionIndex()]);
    } else if (event.key === 'Escape') {
      this.suggestions.set([]);
    }
  }

  formatTemperatureUnit(unit: TemperatureUnit): string {
    return unit === 'G_FAH' ? '°F' : '°C';
  }

  private loadForecast(): void {
    const municipality = this.selectedMunicipality();
    if (!municipality) {
      return;
    }
    this.forecastRequests.next({
      municipalityId: municipality.id,
      unit: this.selectedUnit() || 'G_CEL',
    });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const detail = error.error?.detail;
      if (typeof detail === 'string') {
        return detail;
      }
    }
    return 'No se pudo completar la consulta. Inténtalo de nuevo más tarde.';
  }
}
