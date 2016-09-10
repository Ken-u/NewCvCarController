# CvCarController
###introduction：
```
Because of my poor English,please be considerate~

Use OpenCv for Android，use bluetooth connected with toy,you can regular your phone on the toy,
then your phone would be toy's eye, choose something,it would follow that.if they are too near,it would be back!

Require:You should install OpenCv Manager.apk to use this,and use this to controller a toy car!
```

###It would follow something that you had choosed.Like these:

![image](https://github.com/Ken-u/NewCvCarController/master/gif/1.gif )
![image](https://github.com/Ken-u/NewCvCarController/master/gif/2.gif )
![image](https://github.com/Ken-u/NewCvCarController/master/gif/3.gif )

###Table of the control order:
|direction|order|
|:--:|:--:|
|forward|"6"|
|back|"9"|
|left|"2"|
|right|"4"|


  
     
###some code:

```java
if (ObjectArea<=4000){ /**Distance/Area <=4000**/
                        if((LastX<=CenterLR+35)&&(LastX>=CenterLR-35)) {
                            directions="Center";//OnItemCenter
                            message_LR="6";
                        }
                        else{
                            if(LastX<CenterLR-35) {
                                directions="Left";//OnItemLeft
                                message_LR="2";
                            }
                            if(LastX>CenterLR+35) {
                                directions="Right";//OnItemRight
                                message_LR="4";
                            }
                        }

                        if ((LastY<=CenterUD+35)&&(LastY>=CenterUD-35)){
                            directionsUD="Center";
                            message_UD="0";
                        }
                        else{
                            if (LastY<CenterUD-35){
                                directionsUD="Up";
                                message_UD="9";
                            }
                            if (LastY>CenterUD+35){
                                directionsUD="Down";
                                message_UD="6";
                            }
                        }

                    }
                    else if (ObjectArea>=8000) {
                        message_LR="9";
                        directions="Back";
                    }
                    else{
                        message_LR="0";
                        message_UD="0";
                        directions="Stop";
                        directionsUD="Stop";
                    }
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);// Send message
```

#基于OpenCv的识物小车
###介绍
```
使用安卓版的OpenCV，通过蓝牙与硬件小车进行连接，手机可以固定在车上，这样手机便成为了他的眼睛，在屏幕选定物体，他就会跟着物体走，走到一定距离会停下，太近了会后退！
使用前必须安装OpenCV Manager.apk
```
###他会跟随你在屏幕上选定好的物体进行移动，就想上面的gif图中一样：
图片不再重复给出
###蓝牙控制指令列表如下：
|方向|指令|
|:--:|:--:|
|前进|"6"|
|后退|"9"|
|左|"2"|
|右|"4"|
###一些代码如下：
```java
不再重复
```
