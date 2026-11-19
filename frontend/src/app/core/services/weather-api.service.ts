import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Municipality } from '../models/municipality';
import { TemperatureUnit, WeatherForecast } from '../models/weather-forecast';

@Injectable({ providedIn: 'root' })
export class WeatherApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1';

  searchMunicipalities(query: string): Observable<Municipality[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<Municipality[]>(`${this.apiUrl}/municipalities`, { params });
  }

  getNextDayForecast(municipalityId: string, unit: TemperatureUnit): Observable<WeatherForecast> {
    const params = new HttpParams().set('unit', unit);
    return this.http.get<WeatherForecast>(`${this.apiUrl}/forecast/${municipalityId}`, {
      params,
    });
  }
}
