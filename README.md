# TooAi

    Android application utilizing google ML kit android and Jetpack compose.

# Tech Stack and Libraries

- [Kotlin](https://kotlinlang.org/) : First class and official programming language for Android
  development.
- [Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) : For managing
  background threads with simplified code and reducing needs for callbacks.
- [Flow](https://kotlinlang.org/docs/reference/coroutines/flow.html) : A cold asynchronous data
  stream that sequentially emits values and completes normally or with an exception.
- [Jetpack]
    - [Compose](https://developer.android.com/jetpack/compose) : Modern toolkit for building native
      UI.
    - [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) : Stores
      UI-related data that isn't destroyed on UI changes.
    - [Room](https://developer.android.com/topic/libraries/architecture/room) : SQLite object
      mapping library.
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) : Dependency
  injection library for Android that reduces the boilerplate of doing manual dependency injection in
  your project.
- [ML Kit](https://developers.google.com/ml-kit) : A mobile SDK that brings Google's machine
  learning expertise to Android and iOS apps in a powerful yet easy-to-use package.

# Architecture

- MVVM Architecture (View - DataBinding - ViewModel - Model)
- Repository pattern

# Screenshots

|                           Home                           |                           Image Segmentation                           |                           QR code Scanner                           |
|:--------------------------------------------------------:|:----------------------------------------------------------------------:|:-------------------------------------------------------------------:|
| <img alt="Home" src="screenshots/home.jpg" width="250"/> | <img alt="Home" src="screenshots/image-segmentation.jpg" width="250"/> | <img alt="Home" src="screenshots/qr-code-scanner.jpg" width="250"/> |

|                            Text Recognition                            |
|:----------------------------------------------------------------------:|
| <img alt="Home" src="screenshots/text-recognization.jpg" width="250"/> |


#License

```
MIT License

