# Bitutorial  [![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/sindresorhus/awesome) <img src="https://www.cleveroad.com/public/comercial/label-android.svg" height="19"> <a href="https://www.cleveroad.com/?utm_source=github&utm_medium=label&utm_campaign=contacts"><img src="https://www.cleveroad.com/public/comercial/label-cleveroad.svg" height="19"></a>
![Header image](/images/header.png)

## Meet Crumbling tutorial for Android Apps by Cleveroad
Here comes a new Android library for those who are tired of old boring image sliding in mobile apps. Bitutorial is a simple way to add a unique transition between slides. Whether your application supports a sliding tutorial or image change feature, you certainly want to create a memorable experience. 


![Demo image](/images/demo.gif)
<br/>
###### Also you can watch the animation of the <strong><a target="_blank" href="https://www.youtube.com/watch?v=fjbZTLP6xNI&feature=youtu.be">Crumbling tutorial for Android on YouTube</a></strong> in HD quality.

Try Bitutorial — an easy to use open-source library for the Android platform. The crumbling view of horizontal sliding creates elegant motion and all the elements (images, fonts) of the library are truly customizable, so it can fit an app of any kind.

If you want to stand out from the crowd of similar apps, add this unusual view to your in-app transitions.


[![Awesome](/images/logo-footer.png)](https://www.cleveroad.com/?utm_source=github&utm_medium=label&utm_campaign=contacts)
<br/>

## Setup and usage

To include this library to your project add dependency in **build.gradle** file:

```groovy
    dependencies {
        compile 'com.cleveroad:splittransformation:0.9.0'
    }
```

Then you need to wrap your pager adapter with TransformationAdapterWrapper:

```JAVA
    TransformationAdapterWrapper wrapper = TransformationAdapterWrapper
        .wrap(getContext(), adapter)
        // rows x column = total number of pieces. Larger number of pieces impacts on performance.
        .rows(...)
        .columns(...)
        // Maximum size of spacing between pieces.
        .piecesSpacing(...)
        // Translation for splited pieces.
        .translationX(...)
        .translationY(...)
        // Add top margin for view. Preffer this method instead of setting margin to your view
        // because transformer will split empty space into pieces too.
        .marginTop(...)
        // scale factor for generated bitmaps. Use this if you are facing any OOM issues.
        .bitmapScale(...)
        // If you're using complex views with dynamicaly changed content (like edit texts, lists, etc)
        // you should provide your own complex view detector that will return true for such complex views.
        // Every time user swipes pager, transformer will regenerate and split bitmap for view (at the start of swipe gesture)
        // so make sure detector returns true only if view is a complex one.
        .complexViewDetector(...)
        // You can set your own factory that produces bitmap transformers. Default implementation: splitting view into pieces
        .bitmapTransformerFactory(...)
        .build();
```

Then pass this wrapper to your view pager.

<br />
## Changelog

| Version | Changes                         |
| --- | --- |
| v.0.9.0 | First public release            |

<br />
## Support

If you have any other questions regarding the use of this library, please contact us for support at info@cleveroad.com (email subject: "Bitutorial. Support request.") 

<br />
## License
* * *
    The MIT License (MIT)
    
    Copyright (c) 2016 Cleveroad Inc.
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
