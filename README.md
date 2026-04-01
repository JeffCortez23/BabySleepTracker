# 🍼 Baby Sleep Tracker (Khale's Sleep Tracker)

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Status](https://img.shields.io/badge/Status-Finalizado-success?style=for-the-badge)

> **Asistente personal para padres modernos: registra el sueño y los cambios de pañal de tu bebé con un solo toque y sin esfuerzo nocturno.**

**Baby Sleep Tracker** es una aplicación móvil nativa desarrollada para facilitar la vida de los padres cansados. Nace de la necesidad de llevar un control preciso de las siestas y noches del bebé sin tener que lidiar con interfaces complejas a las 3 de la mañana. 

La aplicación destaca por su diseño ergonómico y moderno (**Glassmorphism**), un flujo condensado en un **Dashboard Premium** de una sola pantalla, animaciones fluidas, respuestas hápticas y sincronización en la nube en tiempo real mediante **Firebase**.

---

## 📱 Características Principales

* ⏱️ **Temporizador en Tiempo Real:** Registra siestas (🌤️) y noches (🌜) con un solo toque. El temporizador en tamaño gigante permite ver la duración del sueño de un vistazo.
* 📊 **Dashboard Premium Unificado:** Gráfica de barras animada con la tendencia semanal y un historial limpio, todo integrado en una vista con *scroll* fluido.
* 🧷 **Registro Rápido de Pañales:** Interfaz "1-Tap" para registrar cambios (Orina, Sólido o Ambos) sin navegar por menús complejos.
* ✏️ **Gestión Total de Datos:** Posibilidad de editar la hora de inicio/fin de cualquier sesión, añadir registros manuales o eliminar entradas erróneas.
* 🏆 **Sistema de Logros Semanales:** Gamificación sutil para mantener la motivación de los padres (ej. "Noche de Paz", "Oso Hibernando", "Siesta Relámpago").
* 🌙 **UI/UX Cuidados al Detalle:** * Soporte nativo para **Modo Claro y Modo Oscuro**.
  * **Feedback Háptico:** Pequeñas vibraciones al interactuar con la app para confirmación física a oscuras.
  * **Snackbars Estilizados:** Mensajes de confirmación visual no intrusivos en forma de burbujas flotantes.

---

## 🛠️ Tecnologías y Arquitectura

Esta aplicación está construida utilizando el stack más moderno del ecosistema Android:

* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI 100% Declarativa)
* **Arquitectura:** **MVVM** (Model-View-ViewModel) con manejo de estado unidireccional (`StateFlow`).
* **Asincronía:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) y [Flow](https://kotlin.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/).
* **Base de Datos:** [Firebase Firestore](https://firebase.google.com/docs/firestore) (Base de datos NoSQL con listeners en tiempo real).
* **Animaciones:** Compose Animations (`animateFloatAsState`, `animateContentSize`).

---

## 🗂️ Estructura del Proyecto

El código fuente sigue los principios de *Clean Architecture* adaptados a Jetpack Compose:

* **`MainActivity.kt`**: Controlador principal de la UI. Contiene la estructura del Dashboard, los modales interactivos y las animaciones.
* **`viewmodel/`**: 
  * `SleepViewModel.kt`: Gestiona la lógica de negocio, expone el estado de la UI y se comunica con el repositorio.
* **`data/`**:
  * `model/`: Entidades de datos (`SleepSession`, `DiaperChange`, `MilestoneUtils`).
  * `repository/`: Capa de abstracción de datos. Contiene la interfaz y la implementación directa de Firebase (`FirebaseSleepRepository`).
* **`ui/`**: 
  * `components/`: Componentes visuales reutilizables como `LiquidCard`.
  * `theme/`: Archivos de configuración de diseño (Colores pastel, tipografía y Theme general).
* **`utils/`**: 
  * `SleepUtils.kt`: Funciones de formateo de tiempo y cálculos matemáticos.

---

## 🚀 Instalación y Uso Local

Para ejecutar este proyecto en tu entorno local:

1. **Clona el repositorio:**
   ```bash
   git clone [https://github.com/JeffCortez23/babysleeptracker.git](https://github.com/JeffCortez23/babysleeptracker.git)

2. **Abre el proyecto en Android Studio.**

3. **Configura Firebase:**
* Crea un proyecto en la Consola de Firebase.
* Registra una app Android con el paquete com.elyefris.khalessleeptracker.
* Habilita Firestore Database en modo de prueba.
* Descarga tu archivo google-services.json y colócalo en el directorio app/ de tu proyecto. (Nota: Este archivo está en el .gitignore por seguridad).

4. *Sincroniza el proyecto con Gradle y ejecuta la app en un emulador o dispositivo físico.*

## 👨‍💻 Autor
**Renzo Jeffrey Cortez Laura** Estudiante de Ingeniería de Sistemas e Informática - UNAM (A.K.A. ElYefris)   
