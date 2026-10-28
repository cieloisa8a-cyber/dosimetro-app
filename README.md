# Dosímetro Laboral - Aplicación de Monitoreo de Ruido Ocupacional

Esta aplicación es una herramienta profesional diseñada para ingenieros de sonido y especialistas en salud ocupacional. Permite medir el nivel de presión sonora (SPL) con ponderación A y calcular la dosis de ruido acumulada según los criterios NIOSH y OSHA.

## 🚀 Características principales
- **Medición SPL en tiempo real:** Rango de 35 a 110 dB(A).
- **Ponderación A (A-Weighting):** Filtro digital IIR que simula la respuesta del oído humano (ISO 226).
- **Dosimetría Dual:** Selección entre criterios NIOSH (85dB/3dB) y OSHA (90dB/5dB).
- **Alertas Inteligentes:** Aviso visual al 50% de la dosis y vibratorio/sonoro al 100%.
- **Calibración Manual:** Ajuste de offset para comparación con sonómetros patrón.
- **Exportación de Datos:** Generación de reportes CSV detallados.

## 🛠️ Fundamento Técnico
La aplicación utiliza la API `AudioRecord` para capturar audio a 44100 Hz. El procesamiento se basa en:
1. **Cálculo de RMS:** Mediante la raíz cuadrada de la media de los cuadrados de las muestras digitales.
2. **Transformación Logarítmica:** Conversión de amplitud lineal a decibelios (dB).
3. **Integración de Dosis:** Cálculo dinámico basado en el tiempo de exposición permitido para cada nivel detectado.

## 📦 Instalación (APK)
Puedes encontrar el archivo APK listo para instalar en la carpeta `/release` de este repositorio o generarlo directamente desde Android Studio (Build > Build APK).

## 👥 Autores
- [Nombre del Integrante 1]
- [Nombre del Integrante 2]

---
*Proyecto desarrollado para la Universidad de San Buenaventura - Ingeniería de Sonido.*
