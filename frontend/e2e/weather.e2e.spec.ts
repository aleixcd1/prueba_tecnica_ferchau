import { expect, test } from '@playwright/test';

test('searches a municipality and refreshes the next-day forecast when the unit changes', async ({
  page,
}) => {
  await page.route('**/api/v1/municipalities?**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([{ id: '28079', nombre: 'Madrid', provincia: 'Madrid' }]),
    });
  });
  await page.route('**/api/v1/forecast/**', async (route) => {
    const unit = new URL(route.request().url()).searchParams.get('unit');
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        mediaTemperatura: unit === 'G_FAH' ? 68 : 20,
        unidadTemperatura: unit,
        probPrecipitacion: [
          { probabilidad: 5, periodo: '00-06' },
          { probabilidad: 10, periodo: '06-12' },
          { probabilidad: 10, periodo: '12-18' },
          { probabilidad: 5, periodo: '18-24' },
        ],
      }),
    });
  });

  await page.goto('/');
  await page.getByRole('combobox', { name: 'Municipio' }).fill('Madrid');
  await page.getByRole('option', { name: /Madrid\s*,\s*Madrid/ }).click();
  await expect(page.getByText('20 °C')).toBeVisible();

  await page.getByLabel('Unidad de temperatura (opcional)').selectOption('G_FAH');
  await expect(page.getByText('68 °F')).toBeVisible();
  await expect(page.getByRole('cell', { name: '00-06' })).toBeVisible();
});
