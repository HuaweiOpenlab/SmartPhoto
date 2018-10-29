#ifndef _ERROR_CODE_H
#define _ERROR_CODE_H

typedef enum {
    NO_ERROR = 0,
    MODEL_NAME_LEN_ERROR = 1,        //模型名称长度错误(模型长度需要为1-128)
    MODEL_DIR_ERROR = 2,             //模型文件路径路径为空
    ODEL_SECRET_KEY_ERROR = 3,       //模型文件解密秘钥长度错误(长度不为0或者64)
    MODEL_PARA_SECRET_KEY_ERROR = 4, //模型参数文件解密秘钥长度错误(长度不能够为0或者64)
    FRAMEWORK_TYPE_ERROR = 5,        //模型框架类型(tensorflow/caffe)选择错误(框架选择不为tensorflow或者caffe或者KALDI)
    MODEL_TYPE_ERROR = 6,            //在线或离线模型类型选择错误(模型类型选择不为在线和离线)
    IPU_FREQUENCY_ERROR = 7,         //IPU频率设置不正确(频率设置不为LOW、Normal、Hign)
    MODEL_NUM_ERROR = 8,             //加载的模型数量错误(模型数量为0或者超过20)
    MODEL_SIZE_ERROR = 9,            //模型大小错误(模型大小为0)
    TIMEOUT_ERROR = 10,              //超时时间设置错误(设置超时时间超过60000ms)
    INPUT_DATA_SHAPE_ERROR = 11,     //输入数据形状错误(n*c*h*w为0)
    OUTPUT_DATA_SHAPE_ERROR = 12,    //输出数据形状错误(n*c*h*w为0)
    INPUT_DATA_NUM_ERROR = 13,       //输入数据的数量错误(输入数据数量为0或者超过20)
    OUTPUT_DATA_NUM_ERROR = 14,      //输出数据的数量错误(输出数据数量为0或者超过20)
    MODEL_MANAGER_TOO_MANY_ERROR = 15, //创建的模型管家实例超过最大值(单个进程创建了三个以上client)
    MODEL_NAME_DUPLICATE_ERROR = 18,   //模型名称重复错误(多个模型中文件名重复)
    HIAI_SERVER_CONNECT_ERROR = 19,   //hiaiserver连接失败(hiaiserver服务未启动)
    HIAI_SERVER_CONNECT_IRPT = 20,    //hiaiserver连接中断
    MODEL_TENSOR_SHAPE_NO_MATCH = 500, //模型尺寸不匹配(输入输出的nchw和模型的nchw不匹配)
    EXPIRATION_FUCNTION = 999,         //接口过期
    INTERNAL_ERROR = 1000              //内部错误
} HIAI_ERROR_CODE;

#endif //_ERROR_CODE_H
