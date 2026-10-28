# 🎧 Dosímetro

**Aplicación Android para el monitoreo de ruido ocupacional**

Dosímetro básico y práctico, desarrollado con fines educativos, que mide el nivel de presión sonora con ponderación A y calcula la dosis de ruido acumulada según los criterios **NIOSH** y **OSHA**, directamente desde el micrófono del celular.

> ⚠️ **Uso educativo.** El micrófono de un teléfono no es un instrumento de Clase 1 ni Clase 2 según IEC 61672. Los resultados sirven para aprender y para hacer estimaciones, no para evaluaciones legales de exposición. Para eso se necesita un dosímetro o sonómetro certificado y calibrado.

---

## 🚀 Características

### Medición
- **Nivel sonoro en tiempo real** en dB(A), con rango de trabajo de 35 a 110 dB(A).
- **Ponderación A** mediante un filtro digital IIR, según la curva definida en IEC 61672-1.
- **Gráfica de historial** con los últimos 40 segundos de nivel y una línea de referencia en 85 dB.
- **Calibración manual** con un offset ajustable para igualar la lectura a un sonómetro patrón.

### Dosimetría
- **Doble criterio** seleccionable:

  | Criterio | Nivel de referencia (8 h) | Tasa de intercambio |
  |---|---|---|
  | NIOSH | 85 dB(A) | 3 dB |
  | OSHA | 90 dB(A) | 5 dB |

- **Alertas progresivas:** aviso visual al 50 % de la dosis y aviso vibratorio y sonoro al 100 %.

### Gestión de mediciones
- **Nombre personalizado** para cada medición, con un nombre por defecto basado en la fecha y la hora.
- **Exportación a CSV** con el nombre elegido, sin sobrescribir archivos existentes (agrega `_1`, `_2`…).
- **Carpeta de destino a elección** del usuario mediante el selector de carpetas del sistema.
- **Confirmación antes de detener** para evitar cortar una medición por accidente.

### Interfaz
- Tema claro con acento periwinkle y tipografía **Bricolage Grotesque**.
- Diseño adaptable a orientación vertical y horizontal, **sin perder la medición al girar el teléfono**.

---

## 🛠️ Fundamento técnico

La captura se hace con la API `AudioRecord` a **44 100 Hz**. Cada bloque de muestras pasa por esta cadena:

```
Micrófono → AudioRecord (44.1 kHz) → Filtro A (IIR) → RMS → dB + offset de calibración → Dosis
```

### 1. Valor RMS

$$x_{rms} = \sqrt{\frac{1}{N}\sum_{n=1}^{N} x[n]^2}$$

### 2. Nivel en decibelios

$$L = 20 \log_{10}\left(\frac{x_{rms}}{x_{ref}}\right) + C$$

Donde $C$ es el offset de calibración obtenido al comparar con un sonómetro patrón.

### 3. Tiempo de exposición permitido

Para cada nivel $L$, el tiempo máximo permitido (en horas) es:

$$T = \frac{8}{2^{(L - L_c)/q}}$$

con $L_c = 85$ dB y $q = 3$ dB para NIOSH, o $L_c = 90$ dB y $q = 5$ dB para OSHA.

### 4. Dosis acumulada

$$D = 100 \cdot \sum_{i} \frac{C_i}{T_i} \quad [\%]$$

Donde $C_i$ es el tiempo real expuesto al nivel $L_i$. Una dosis del **100 %** equivale a la exposición máxima permitida en una jornada de 8 horas.

---

## 📦 Instalación

### Desde Android Studio
1. Clona el repositorio:
   ```bash
   git clone https://github.com/cieloisa8a-cyber/dosimetro-app.git
   ```
2. Ábrelo en Android Studio y espera a que termine la sincronización de Gradle.
3. Conecta un teléfono con la depuración USB activada y presiona **Run ▶**.
4. Para generar el instalador: **Build → Build APK(s)**. El APK queda en `app/build/outputs/apk/`.

### Permisos
La app solicita acceso al **micrófono** (`RECORD_AUDIO`) la primera vez que se inicia una medición. La carpeta de exportación se autoriza desde el selector del sistema, así que no requiere permisos de almacenamiento adicionales.

---

## 📖 Cómo usarla

1. **Calibra** (opcional, pero recomendado): en ajustes, ajusta el offset hasta que la lectura coincida con un sonómetro patrón.
2. **Elige el criterio**: NIOSH u OSHA.
3. Toca **Comenzar**, ponle un nombre a la medición y confirma la carpeta de destino.
4. Deja el teléfono en la zona de trabajo, idealmente cerca del oído del trabajador y sin tapar el micrófono.
5. Toca **Detener** y confirma. El CSV queda guardado en la carpeta elegida.

---

## 🧰 Tecnologías

- **Kotlin** + **Android Studio**
- **Gradle (Kotlin DSL)**
- `AudioRecord` para la captura de audio
- Storage Access Framework para la exportación de archivos

---

## 👥 Autores

- Cielo Ochoa
- Juan de los Palotes
- Mel Osako

---

<p align="center">
  <i>Proyecto desarrollado para la Universidad de San Buenaventura · Ingeniería de Sonido</i>
</p>
