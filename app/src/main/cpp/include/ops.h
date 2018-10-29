//
// Created by baiguochao on 2018/6/27.
//

#ifndef MYAPPLICATION_OPS_H
#define MYAPPLICATION_OPS_H

#include <cmath>

/**
 * softmax operation
 * @param buffer data buffer
 * @param size   data buffer size
 */
void static softmax(float *buffer, int size) {

    double max = buffer[0];

    //find max value
    for (int i = 0; i < size; i++) {
        if (buffer[i] > max)
            max = buffer[i];
    }

    //softmax
    double sum = 0;
    for (int i = 0; i < size; i++) {
        buffer[i] = exp(buffer[i] - max);
        sum += buffer[i];
    }

    for (int i = 0; i < size; i++) {
        buffer[i] = buffer[i] / sum;
    }
}

#endif //MYAPPLICATION_OPS_H
