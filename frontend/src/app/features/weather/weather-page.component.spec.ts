import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Municipality } from '../../core/models/municipality';
import { WeatherApiService } from '../../core/services/weather-api.service';
import { WeatherPageComponent } from './weather-page.component';

describe('WeatherPageComponent', () => {
  it('loads the selected municipality forecast in the default Celsius unit', async () => {
    const municipality: Municipality = { id: '28079', nombre: 'Madrid', provincia: 'Madrid' };
    const api = {
      searchMunicipalities: () => of([municipality]),
      getNextDayForecast: () =>
        of({
          mediaTemperatura: 20,
          unidadTemperatura: 'G_CEL' as const,
          probPrecipitacion: [{ probabilidad: 5, periodo: '00-06' }],
        }),
    };
    await TestBed.configureTestingModule({
      imports: [WeatherPageComponent],
      providers: [{ provide: WeatherApiService, useValue: api }],
    }).compileComponents();

    const fixture = TestBed.createComponent(WeatherPageComponent);
    fixture.componentInstance.selectMunicipality(municipality);
    fixture.detectChanges();

    expect(fixture.componentInstance.forecast()?.mediaTemperatura).toBe(20);
    expect(fixture.nativeElement.textContent).toContain('Predicción para mañana en Madrid');
  });
});
