# 中文版
## <center><font>HiAI 模型集成应用场景和开发指南</font></center>
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;该开发指南可以指导Android手机应用开发者将常见深度学习框架(Caffe、Tensorflow、Coreml、PaddlePaddle)训练的AI模型集成到华为NPU手机中。
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;最近小编闲来无事跑到附近动物园转了转，动物园里奇珍异兽数不胜数。红翻石鹬、红脚鹬、蛎鹬、沙狐、北极狐…弄得小编是一脸的懵，分不清谁是谁。无奈小编只能每次拍小动物们之前先拍名牌上的动物名称，以便日后翻看图片能够对上号，可是这样一来小编翻看图片时都处于来回翻看动物名称的疯狂状态。有没有和小编同样遭遇的你，SmartPhoto App就可以解决这一问题。该应用能够在拍照时自动识别物体，并在拍出的照片上打上水印标签。如下图：

![image](https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/%E5%8C%97%E6%9E%81%E7%8B%90%E7%8B%B8.png?raw=true) ![image](https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/%E6%B2%99%E7%8B%90%E7%8B%B8.png?raw=true)

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;巧妇难为无米之炊，我们先得准备自己的AI模型，这里小编以CoremlStore的[MobileNet](https://coreml.store/mobilenet)模型为例，演示如何集成图像识别的AI模型到App中。


### 第一步：首先下载安装Android Studio插件，见下图，搜索栏中输入“DevEco IDE”关键字。
<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/IDE_install.png?raw=true" div align=center/>


### 第二步：进入DevEco插件，并拖入模型到图中所示位置，进入参数配置界面，输入相关参数，点击Run按钮。

<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelConvert_1.png?raw=true" div align=center/>

<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelConvert_2.png?raw=true" width = "275" height = "460" div align=left /><img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelConvert_3.png?raw=true" width = "275" height = "460" div align=center /><img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelConvert_4.png?raw=true" width = "275" height = "460" div align=right />

*注意，上图第6步中需选择DDK版本， DDK版本和手机之间对应关系详见选择框右边的“问号”按钮*


&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;工具会帮助自动生成的Java API，如下图所示，Java文件的存放路径在参数配置界面中设置，类名称由模型名称+“Model”字符串构成。

<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/api.png?raw=true" div align=center/>


### 第三步：API的使用

（1）在onCreate函数中加载模型
<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelLoad.png?raw=true" div align=left/>

（2）在获取到图片数据之后对图片进行预测
<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelPredict.png?raw=true" div align=left/>

（3）模型结束使用时在onDestroy()函数中卸载模型
<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/ModelUnload.png?raw=true" div align=left/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;到此，AI模型在App中的集成过程就结束了，有没有感觉很简单？总结起来就是“下载插件，拖入模型，再写三句代码”。
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;【提示】该Demo目前只支持麒麟980手机，小编开发用的是华为Mate20手机，没有Mate20手机的小伙伴，可以在上述Android Studio插件中免费申请Mate20远程真机进行调试。



### 免费远程真机的使用步骤如下:

<img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/RemoteDevice_1.png?raw=true" div align=center/>

 <img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/RemoteDevice_2.png?raw=true" width = "350" height = "460" div align=left/>
<div align=center>
 <img src="https://github.com/HuaweiOpenlab/SmartPhoto/blob/master/readme_image/RemoteDevice_3.png?raw=true" width = "400" height = "700" /></div>



邮箱反馈途径：deveco@huawei.com



