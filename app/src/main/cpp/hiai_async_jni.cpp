#include <jni.h>
#include <string>
#include <memory.h>
#include "include/HIAIModelManager.h"
#include "include/ErrorCode.h"
#include "include/ops.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <sstream>
#include <unistd.h>

#define LOG_TAG "ASYNC_DDK_MSG"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;

static HIAI_ModelManager *modelManager = NULL;

static HIAI_TensorBuffer *inputtensor = NULL;

static HIAI_TensorBuffer *outputtensor = NULL;

static HIAI_ModelBuffer *modelBuffer = NULL;

static HIAI_ModelManagerListener listener;
static jclass callbacksClass;
static jobject callbacksInstance;
JavaVM *jvm;

float time_use;
struct timeval tpstart, tpend;


static int input_N = 0;
static int input_C = 0;
static int input_H = 0;
static int input_W = 0;
static int output_N = 0;
static int output_C = 0;
static int output_H = 0;
static int output_W = 0;

//get Input and Output N C H W from model  after loading success the model
static void getInputAndOutputFromModel(const char *modelName){
    HIAI_ModelTensorInfo* modelTensorInfo = HIAI_ModelManager_getModelTensorInfo(modelManager, modelName);
    if (modelTensorInfo == NULL){
        LOGE("HIAI_ModelManager_getModelTensorInfo failed!!");
        return ;
    }

    /**
     * if your model have muli-input and muli-output
     * you can get N C H W from model like as below:
     *
     for (int i = 0; i < modelTensorInfo->input_cnt; ++i)
    {
        LOGI("input[%u] N: %u-C: %u-H: %u-W: %u\n", i, modelTensorInfo->input_shape[i*4], modelTensorInfo->input_shape[i*4 + 1],
               modelTensorInfo->input_shape[i*4 + 2], modelTensorInfo->input_shape[i*4 + 3]);


        HIAI_TensorBuffer* input = HIAI_TensorBuffer_create(modelTensorInfo->input_shape[i*4], modelTensorInfo->input_shape[i*4 + 1],
                                                            modelTensorInfo->input_shape[i*4 + 2], modelTensorInfo->input_shape[i*4 + 3]);
     }
     */

    LOGI("input count: %u  output:%u\n",  modelTensorInfo->input_cnt,modelTensorInfo->output_cnt);

    //get N C H W from model, The case use 1 input and 1 output ,So we take a simplified approach here
    LOGI("input N: %u-C: %u-H: %u-W: %u\n",  modelTensorInfo->input_shape[0], modelTensorInfo->input_shape[1],
         modelTensorInfo->input_shape[2], modelTensorInfo->input_shape[3]);
    input_N = modelTensorInfo->input_shape[0];
    input_C = modelTensorInfo->input_shape[1];
    input_H = modelTensorInfo->input_shape[2];
    input_W = modelTensorInfo->input_shape[3];

    LOGI("output N: %u-C: %u-H: %u-W: %u\n",  modelTensorInfo->output_shape[0], modelTensorInfo->output_shape[1],
         modelTensorInfo->output_shape[2], modelTensorInfo->output_shape[3]);
    output_N = modelTensorInfo->output_shape[0];
    output_C = modelTensorInfo->output_shape[1];
    output_H = modelTensorInfo->output_shape[2];
    output_W = modelTensorInfo->output_shape[3];

    if(modelTensorInfo != NULL){
        HIAI_ModelManager_releaseModelTensorInfo(modelTensorInfo);
        modelTensorInfo = NULL;
    }
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    LOGE("AYSNC JNI layer JNI_OnLoad");

    jvm = vm;

    return JNI_VERSION_1_6;
}


void onLoadDone(void *userdata, int taskId) {

    LOGE("AYSNC JNI layer onLoadDone:", taskId);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (callbacksInstance != NULL) {
        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onStartDone", "(I)V");
        env->CallVoidMethod(callbacksInstance, onValueReceived, taskId);
    }
}

void onRunDone(void *userdata, int taskStamp) {
    gettimeofday(&tpend, NULL);
    time_use = 1000000 * (tpend.tv_sec - tpstart.tv_sec) + tpend.tv_usec - tpstart.tv_usec;

    LOGI("AYSNC infrence time %f ms.", time_use / 1000);
    LOGE("AYSNC JNI layer onRunDone taskStamp: %d", taskStamp);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    float *outputBuffer = (float *) listener.userdata;

    //Todo: Modify to your own data(category)
    int outputSize = 1000;

    softmax(outputBuffer,outputSize);



    jfloatArray result = env->NewFloatArray(outputSize);

    env->SetFloatArrayRegion(result, 0, outputSize, outputBuffer);

   if (callbacksInstance != NULL) {

        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onRunDone",
                                                     "(I[LF;)V");
        env->CallVoidMethod(callbacksInstance, onValueReceived, taskStamp, result);
    }
if (inputtensor != NULL) {
        HIAI_TensorBuffer_destroy(inputtensor);
        inputtensor = NULL;
    }

    if (outputtensor != NULL) {
        HIAI_TensorBuffer_destroy(outputtensor);
        outputtensor = NULL;
    }
}


void onUnloadDone(void *userdata, int taskStamp) {
    LOGE("JNI layer onUnloadDone:", taskStamp);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (callbacksInstance != NULL) {
        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onStopDone", "(I)V");
        env->CallVoidMethod(callbacksInstance, onValueReceived, taskStamp);
    }


    HIAI_ModelManager_destroy(modelManager);

    modelManager = NULL;

    listener.onRunDone = NULL;
    listener.onUnloadDone = NULL;
    listener.onTimeout = NULL;
    listener.onServiceDied = NULL;
    listener.onError = NULL;
    listener.onLoadDone = NULL;
}


void onTimeout(void *userdata, int taskStamp) {
    LOGE("JNI layer onTimeout:", taskStamp);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (callbacksInstance != NULL) {
        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onTimeout", "(I)V");
        env->CallVoidMethod(callbacksInstance, onValueReceived, taskStamp);
    }
}

void onError(void *userdata, int taskStamp, int errCode) {
    LOGE("JNI layer onError:", taskStamp);

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (callbacksInstance != NULL) {
        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onError", "(II)V");
        env->CallVoidMethod(callbacksInstance, onValueReceived, taskStamp, errCode);
    }
}

void onServiceDied(void *userdata) {
    LOGE("JNI layer onServiceDied:");

    JNIEnv *env;

    jvm->AttachCurrentThread(&env, NULL);

    if (callbacksInstance != NULL) {
        jmethodID onValueReceived = env->GetMethodID(callbacksClass, "onServiceDied", "()V");
        env->CallVoidMethod(callbacksInstance, onValueReceived);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_huawei_hiaidemo_ModelSDK_ModelManager_registerListenerJNI(JNIEnv *env, jclass type,
                                                          jobject callbacks) {

    callbacksInstance = env->NewGlobalRef(callbacks);
    jclass objClass = env->GetObjectClass(callbacks);
    if (objClass) {
        callbacksClass = reinterpret_cast<jclass>(env->NewGlobalRef(objClass));
        env->DeleteLocalRef(objClass);
    }

    listener.onLoadDone = onLoadDone;
    listener.onRunDone = onRunDone;
    listener.onUnloadDone = onUnloadDone;
    listener.onTimeout = onTimeout;
    listener.onError = onError;
    listener.onServiceDied = onServiceDied;

    modelManager = HIAI_ModelManager_create(&listener);
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_huawei_hiaidemo_ModelSDK_ModelManager_loadModelAsync(JNIEnv *env, jclass type,
                                                     jstring jmodelName, jobject assetManager) {
    const char *modelName = env->GetStringUTFChars(jmodelName, 0);

    char modelname[128] = {0};

    strcat(modelname, modelName);
    strcat(modelname, ".cambricon");

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    LOGI("Attempting to load model...\n");

    LOGE("model name is %s", modelname);

    AAsset *asset = AAssetManager_open(mgr, modelname, AASSET_MODE_BUFFER);

    if (nullptr == asset) {
        LOGE("AAsset is null...\n");
    }

    const void *data = AAsset_getBuffer(asset);

    if (nullptr == data) {
        LOGE("model buffer is null...\n");
    }

    off_t len = AAsset_getLength(asset);

    if (0 == len) {
        LOGE("model buffer length is 0...\n");
    }


    HIAI_ModelBuffer *modelBuffer = HIAI_ModelBuffer_create_from_buffer(modelName,
                                                                        (void *) data, len,
                                                                        HIAI_DevPerf::HIAI_DEVPREF_HIGH);
    HIAI_ModelBuffer *modelBufferArray[] = {modelBuffer};

    int ret = HIAI_ModelManager_loadFromModelBuffers(modelManager, modelBufferArray, 1);

    LOGE("ASYNC JNI LAYER load model from assets ret = %d", ret);

	getInputAndOutputFromModel(modelName);
	
    env->ReleaseStringUTFChars(jmodelName, modelName);

    AAsset_close(asset);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_huawei_hiaidemo_ModelSDK_ModelManager_runModelAsync(JNIEnv *env, jclass type,
                                                    jstring jmodelName, jfloatArray jbuf) {
    const char *modelName = env->GetStringUTFChars(jmodelName, 0);

    if (NULL == modelManager) {
        LOGE("please load model first");

        return;
    }

    float *dataBuff = NULL;

    if (NULL != jbuf) {
        dataBuff = env->GetFloatArrayElements(jbuf, NULL);
    }

    //Todo: modify input tensor
    inputtensor = HIAI_TensorBuffer_create(input_N, input_C, input_H, input_W);

    HIAI_TensorBuffer *inputtensorbuffer[] = {inputtensor};

    //Todo: modify  output tensor
    outputtensor = HIAI_TensorBuffer_create(output_N, output_C, output_H, output_W);

    HIAI_TensorBuffer *outputtensorbuffer[] = {outputtensor};


    float *inputbuffer = (float *) HIAI_TensorBuffer_getRawBuffer(inputtensor);

    int length = HIAI_TensorBuffer_getBufferSize(inputtensor);

    memcpy(inputbuffer, dataBuff, length);


    gettimeofday(&tpstart, NULL);

    //Todo: modify runModel parameter
    int ret = HIAI_ModelManager_runModel(
            modelManager,
            inputtensorbuffer,
            1,
            outputtensorbuffer,
            1,
            1000,
            modelName);

    LOGE("ASYNC JNI layer runmodel ret: %d", ret);

    float *outputdata = (float *) HIAI_TensorBuffer_getRawBuffer(outputtensor);

    listener.userdata = NULL;

    listener.userdata = outputdata;

    env->ReleaseStringUTFChars(jmodelName, modelName);
    env->ReleaseFloatArrayElements(jbuf, dataBuff, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_huawei_hiaidemo_ModelSDK_ModelManager_unloadModelAsync(JNIEnv *env, jclass type) {
    if (NULL == modelManager) {
        LOGE("please load model first");

        return;
    } else {
        if (modelBuffer != NULL) {
            HIAI_ModelBuffer_destroy(modelBuffer);
            modelBuffer = NULL;
        }

        int ret = HIAI_ModelManager_unloadModel(modelManager);

        LOGE("ASYNC JNI layer unLoadModel ret:%d", ret);
    }
}

