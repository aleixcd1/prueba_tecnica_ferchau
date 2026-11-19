export type TemperatureUnit = 'G_CEL' | 'G_FAH';

export interface PrecipitationProbability {
  probabilidad: number | null;
  periodo: string;
}

export interface WeatherForecast {
  mediaTemperatura: number;
  unidadTemperatura: TemperatureUnit;
  probPrecipitacion: PrecipitationProbability[];
}
