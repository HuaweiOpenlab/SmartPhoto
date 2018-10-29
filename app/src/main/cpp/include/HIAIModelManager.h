#ifndef __HIAI_MODEL_MANAGER_H__
#define __HIAI_MODEL_MANAGER_H__

#include <stdbool.h>

/**
This is the HIAI ModelManager C API:
*/
#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    HIAI_MODELTYPE_ONLINE = 0,
    HIAI_MODELTYPE_OFFLINE
} HIAI_ModelType;

typedef enum {
    HIAI_FRAMEWORK_NONE = 0,
    HIAI_FRAMEWORK_TENSORFLOW,
    HIAI_FRAMEWORK_KALDI,
    HIAI_FRAMEWORK_CAFFE,
    HIAI_FRAMEWORK_INVALID,
} HIAI_Framework;

typedef enum {
    HIAI_DEVPERF_UNSET = 0,
    HIAI_DEVPREF_LOW,
    HIAI_DEVPREF_NORMAL,
    HIAI_DEVPREF_HIGH,
} HIAI_DevPerf;

typedef enum {
    HIAI_DATATYPE_UINT8 = 0,
    HIAI_DATATYPE_FLOAT32 = 1,
} HIAI_DataType;

#define HIAI_DEVPERF_DEFAULT (HIAI_DEVPREF_HIGH)

typedef struct {
    const char* modelNetName;

    const char* modelNetPath;
    bool isModelNetEncrypted;
    const char* modelNetKey;

    const char* modelNetParamPath;
    bool isModelNetParamEncrypted;
    const char* modelNetParamKey;

    HIAI_ModelType modelType;
    HIAI_Framework frameworkType;
    HIAI_DevPerf   perf;
} HIAI_ModelDescription;

#define HIAI_MODEL_DESCRIPTION_INIT { \
    "", \
    "", false, "", \
    "", false, "", \
    HIAI_MODELTYPE_OFFLINE, \
    HIAI_FRAMEWORK_CAFFE, \
    HIAI_DEVPERF_DEFAULT \
}

typedef struct {
    int number;
    int channel;
    int height;
    int width;
    HIAI_DataType dataType;  /* optional */
} HIAI_TensorDescription;

#define HIAI_TENSOR_DESCRIPTION_INIT {0, 0, 0, 0, HIAI_DATATYPE_FLOAT32}

typedef struct HIAI_ModelManagerListener_struct
{
    void (*onLoadDone)(void* userdata, int taskStamp);
    void (*onRunDone)(void* userdata, int taskStamp);
    void (*onUnloadDone)(void* userdata, int taskStamp);
    void (*onTimeout)(void* userdata, int taskStamp);
    void (*onError)(void* userdata, int taskStamp, int errCode);
    void (*onServiceDied)(void* userdata);

    void* userdata;
} HIAI_ModelManagerListener;

#define HIAI_MODEL_MANAGER_LISTENER_INIT {NULL, NULL, NULL, NULL, NULL, NULL, NULL}


typedef struct HIAI_ModelBuffer HIAI_ModelBuffer;

/*
 * 从路径中加载模型，创建HIAI_ModelBuffer(用于从应用层sdcard中加载模型)
 * @param name：要加载的模型名称(不包含文件后缀名称.例如要加载 "alexnet.cambricon", 请输入"alexnet")
 * @param path：要加载的模型所在路径
 * @param perf：NPU频率，对应高中低三挡
 * @return 返回HIAI_ModelBuffer
 */
HIAI_ModelBuffer* HIAI_ModelBuffer_create_from_file(const char* name, const char* path, HIAI_DevPerf perf);

/**
 * 读取模型数据后加载模型，创建HIAI_ModelBuffer(用于从应用层assets中加载模型)
 * @param name     要加载的模型名称(不包含文件后缀名称.例如要加载 "alexnet.cambricon", 请输入"alexnet")
 * @param modelBuf 模型数据地址
 * @param size     模型数据长度
 * @param perf     PU频率，对应高中低三挡
 * @return         返回HIAI_ModelBuffer
 */
HIAI_ModelBuffer* HIAI_ModelBuffer_create_from_buffer(const char* name, void* modelBuf, int size, HIAI_DevPerf perf);

const char* HIAI_ModelBuffer_getName(HIAI_ModelBuffer* b);
const char* HIAI_ModelBuffer_getPath(HIAI_ModelBuffer* b);
HIAI_DevPerf HIAI_ModelBuffer_getPerf(HIAI_ModelBuffer* b);

int HIAI_ModelBuffer_getSize(HIAI_ModelBuffer* b);

/**
 * 销毁HIAI_ModelBuffer
 * @param b 需要销毁的HIAI_ModelBuffer
 */
void HIAI_ModelBuffer_destroy(HIAI_ModelBuffer* b);



typedef struct HIAI_TensorBuffer HIAI_TensorBuffer;

/**
 * 创建HIAI_TensorBuffer
 * @param n  tensor的batch
 * @param c  tensor的channel
 * @param h  tensor的height
 * @param w  tensor的width
 * @return 返回HIAI_TensorBuffer
 */
HIAI_TensorBuffer* HIAI_TensorBuffer_create(int n, int c, int h, int w);

HIAI_TensorBuffer* HIAI_TensorBuffer_createFromTensorDesc(HIAI_TensorDescription* tensor);

HIAI_TensorDescription HIAI_TensorBuffer_getTensorDesc(HIAI_TensorBuffer* b);

/**
 * 获取模型输入或者输出数据地址
 * @param b 模型输入或输出的HIAI_TensorBuffer
 * @return  模型输入或者输出的数据地址
 */
void* HIAI_TensorBuffer_getRawBuffer(HIAI_TensorBuffer* b);

/**
 * 获取模型输入或者输出数据长度
 * @param b 模型输入或输出的HIAI_TensorBuffer
 * @return  返回模型输入或者输出数据的长度
 */
int HIAI_TensorBuffer_getBufferSize(HIAI_TensorBuffer* b);

/**
 * 销毁HIAI_TensorBuffer
 * @param b 需要销毁的HIAI_TensorBuffer
 */
void HIAI_TensorBuffer_destroy(HIAI_TensorBuffer* b);

typedef struct
{
    int input_cnt;
    int output_cnt;
    int *input_shape;
    int *output_shape;
} HIAI_ModelTensorInfo;


typedef struct HIAI_ModelManager HIAI_ModelManager;

HIAI_ModelTensorInfo* HIAI_ModelManager_getModelTensorInfo(HIAI_ModelManager* manager, const char* modelName);

void HIAI_ModelManager_releaseModelTensorInfo(HIAI_ModelTensorInfo* modelTensor);

HIAI_ModelManager* HIAI_ModelManager_create(HIAI_ModelManagerListener* listener);

/**
 * 销毁模型管家
 * @param manager 模型管理引擎对象接口
 */
void HIAI_ModelManager_destroy(HIAI_ModelManager* manager);

/**
 * 加载模型文件
 * @param manager 模型管理引擎对象
 * @param bufferArray HIAI_ModelBuffer，单模型和多模型均可。
 * @param nBuffers 加载模型的个数(bufferArray数组大小)
 * @return 执行成功，返回0；执行失败，返回相应错误值(参见: ErrorCode.h)。
 */
int HIAI_ModelManager_loadFromModelBuffers(HIAI_ModelManager* manager, HIAI_ModelBuffer* bufferArray[], int nBuffers);

int HIAI_ModelManager_loadFromModelDescriptions(HIAI_ModelManager* manager, HIAI_ModelDescription descsArray[], int nDescs);

/**
 * 模型运行接口
 * @param manager 模型管理引擎对象
 * @param input  模型输入，支持多输入
 * @param nInput 模型输入的个数
 * @param output 模型输出，支持多输出
 * @param nOutput 模型输出的个数
 * @param ulTimeout 超时时间，在同步调用时不生效
 * @param modelName 模型名称(不包含文件后缀名称.例如要运行 "alexnet.cambricon", 请输入"alexnet")
 * @return 执行成功，返回0；执行失败，返回相应错误值(参考 ErrorCode.h)。
 */
int HIAI_ModelManager_runModel(
        HIAI_ModelManager* manager,
        HIAI_TensorBuffer* input[],
        int nInput,
        HIAI_TensorBuffer* output[],
        int nOutput,
        int ulTimeout,
        const char* modelName);

/**
 * 设置模型的输入和输出
 * @param manager 模型管理引擎对象接口
 * @param modelname 模型名称(不包含文件后缀名称.例如要运行 "alexnet.cambricon", 请输入"alexnet")
 * @param input 模型输入，支持多输入
 * @param nInput 模型输入的个数
 * @param output 模型输出，支持多输出
 * @param nOutput 模型输出的个数
 * @return 执行成功，返回0；执行失败，返回相应错误值(参考 ErrorCode.h)。
 */
int HIAI_ModelManager_setInputsAndOutputs(
        HIAI_ModelManager* manager,
        const char* modelname,
        HIAI_TensorBuffer* input[],
        int nInput,
        HIAI_TensorBuffer* output[],
        int nOutput);

/**
 * 启动计算接口
 * @param manager 模型管理引擎对象接口。
 * @param modelname 模型名称(不包含文件后缀名称.例如要运行 "alexnet.cambricon", 请输入"alexnet")
 * @return 执行成功，返回0；执行失败，返回相应错误值(参考 ErrorCode.h)。
 */
int HIAI_ModelManager_startCompute(HIAI_ModelManager* manager, const char* modelname);

/**
 * 卸载模型
 * @param manager 模型管理引擎对象接口
 * @return 执行成功，返回0；执行失败，返回相应错误值(参考 ErrorCode.h)。
 */
int HIAI_ModelManager_unloadModel(HIAI_ModelManager* manager);

/**
 * 获取系统中的DDK版本号
 * @return 执行成功，返回相应的DDK版本号。版本号名称，以<major>.<middle>.<minor>.<point>的形式描述版本。
 * <major>：产品形态，1XX：手机形态，2XX：边缘计算形态，3XX：Cloud形态
 * <middle>：XXX三位数表示，同一产品形态的V版本，如手机形态下，HiAIV100，HiAIV200等
 * <minor>：增量C版本，新增大特性，XXX三位数表示 < point >：B版本或者补丁版本，XXX三位数表示，最后一位非0，表示补丁版本。
 * 例如kirin970系统返回的版本号是100.150.010.010如果返回000.000.000.000，表示此版本不支持NPU加速；
 * 执行失败，返回相应错误值。
 */
char* HIAI_GetVersion();

#ifdef __cplusplus
}  // extern "C"
#endif

#endif  // __HIAI_MODEL_MANAGER_H__
