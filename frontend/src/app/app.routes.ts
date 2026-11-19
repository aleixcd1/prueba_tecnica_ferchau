import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/weather/weather-page.component').then(
        (component) => component.WeatherPageComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
