## `Bird`  

* #### `Rect`
> 用来表示鸟在屏幕中的位置

> Rect类主要用于表示坐标系中的一块矩形区域，
> 这块矩形区域，需要用左上右下两个坐标点表示（left,top,right,bottom）,也可以获取一个Rect实例的Width和Height。  
> 
> ![](/run/media/yuyi/068AE93F8AE92BBD/photo/BirdPhoto.png)

* #### `Bitmap`

> Bitmap位图包括像素以及长、宽、颜色等描述信息

> 位图可以理解为一个画架，把图放到上面然后可以对图片做一些处理。
> 从资源里的加载方法
>
> ```java
> Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.sample); 
> ```

* #### `Matrix`
> 用来对图片进行变换的处理。
* #### `Synchronize*`
> 同步锁，解决并发问题。
>
> -------------------------
* 两个状态的 速度、加速度，角速度，角加速度计算

* ##### 待命状态时，鸟是怎么飞的

> ![](/run/media/yuyi/068AE93F8AE92BBD/photo/BIrd.png)

* ##### 游戏时，鸟是怎么飞的 

> 关键在于实现旋转效果
>
>  1. 计算角速度，然后用速度更新位置
>  2. 分向上飞、和向下坠落两种情况。通过向上向下的速度和加速度计算飞行时间，
然后求出两种情况的角速度

------------------------






## `BirdWorld`

* #### `Cloneable`
> 想要自定义类的对象可以被克隆，我们需要实现 `Cloneable` 接口中的 `clone()` 方法

> 深度克隆与浅度克隆  https://blog.csdn.net/Before_Morning/article/details/49096677 
>
------------------







> ##  `GameView`


#### `Why SurfaceView?`  


* `SurfaceView` 的窗口刷新的时候不需要重绘,

* 应用程序的窗口而 `android` 普通窗口的视图绘制机制是一层一层的，任何一个子元素或者是局部的刷新都会导致整个视图结构全部重绘一次.  


>  1. ###### `View` 与 `SurfaceView` 的区别：  
>
>  * `View` 适用主动更新，`SurfaceView` 适用被动更新，如频繁的刷新
>
>  * `View` 在 `UI` 线程更新，在非 `UI` 线程更新会报错，当在主线程更新 >`view` 时如果耗时过长也会出错。
>
>  * `SurfaceView` 在子线程刷新不会阻塞主线程，适用于界面频繁更新、对帧率要求较高的情况。
>
> * `SurfaceView` 可以控制刷新频率。
>
> * `SurfaceView` 底层利用双缓存机制，绘图时不会出现闪烁问题。  


>  2. ###### 双缓冲机制  
>
>  * 双缓冲技术是游戏开发中的一个重要的技术，主要是为了解决反复局部刷屏带来的闪烁。
>  * 游戏，视频等画面变化较频繁，前面还没有显示完，程序又请求重新绘制，这样屏幕就会不停地闪烁。
>  * 双缓冲技术会把要处理的图片在内存中处理好之后，把要画的东西先画到一个内存区域里，然后整体的一次性画出来，将其显示在屏幕上。  

------

#### `SurfaceView` 的使用

> `SurfaceView` 的双缓冲的机制非常消耗系统内存，`Android` 规定`SurfaceView` 不可见时，会立即销毁 `SurfaceView` 的 `SurfaceHolder`，以达到节约系统资源的目的，所以需要利用 `SurfaceHolder` 的回调函数对 `SurfaceHolder` 进行维护。

> 所以需要用这三个回调函数让我们知道SurfaceHolder的创建、销毁或者改变
  `void surfaceDestroyed(SurfaceHolder holder)`：当 `SurfaceHolder` 被销毁的时候回调。
  `void surfaceCreated(SurfaceHolder holder)`：当 `SurfaceHolder` 被创建的时候回调。
  `void surfaceChange(SurfaceHolder holder)`：当 `SurfaceHolder` 的尺寸或格式发生变化的时候被回调。

------

#### `Runnable`

> 用来创建多线程
> * `Runnable` 接口可以被任何想要被一个线程运行的接口继承实现；继承  `Runnable` 接口的类必须有一个 `run()` 方法
> * `Runnable` 接口被设计的目的是为那些当其处于活跃期时期望运行代码的对象提供一个公共的协议；处于活跃期简单的说就是一个线程已经启动但还没有结束
> * 继承 `Runnable` 接口实现线程，不需继承 `Thread`；而将类本身作为 `Thread` 中的目标 `target`
> * `Runnable` 接口最好不要继承，除非开发者想要更好的扩展此接口的功能

#### `onSingleTapUp`

> 触发条件：一次单独的轻击抬起操作
> 