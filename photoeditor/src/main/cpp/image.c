#include <jni.h>
#include <stdio.h>
#include <setjmp.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>


const uint32_t PGPhotoEnhanceHistogramBins = 256;
const uint32_t PGPhotoEnhanceSegments = 4;

#ifndef MAX
#define MAX(x, y) ((x) > (y)) ? (x) : (y)
#endif
#ifndef MIN
#define MIN(x, y) ((x) < (y)) ? (x) : (y)
#endif

JNIEXPORT void
Java_com_felan_photoeditor_utils_Utilities_calcCDT(JNIEnv *env, jclass class, jobject hsvBuffer,
                                                   jint width, jint height, jobject buffer) {
    float imageWidth = width;
    float imageHeight = height;
    float _clipLimit = 1.25f;

    uint32_t totalSegments = PGPhotoEnhanceSegments * PGPhotoEnhanceSegments;
    uint32_t tileArea = (uint32_t) (floorf(imageWidth / PGPhotoEnhanceSegments) *
                                    floorf(imageHeight / PGPhotoEnhanceSegments));
    uint32_t clipLimit = (uint32_t) MAX(1, _clipLimit * tileArea /
                                           (float) PGPhotoEnhanceHistogramBins);
    float scale = 255.0f / (float) tileArea;

    unsigned char *bytes = (*env)->GetDirectBufferAddress(env, hsvBuffer);

    uint32_t **hist = calloc(totalSegments, sizeof(uint32_t *));
    uint32_t **cdfs = calloc(totalSegments, sizeof(uint32_t *));
    uint32_t *cdfsMin = calloc(totalSegments, sizeof(uint32_t));
    uint32_t *cdfsMax = calloc(totalSegments, sizeof(uint32_t));

    for (uint32_t a = 0; a < totalSegments; a++) {
        hist[a] = calloc(PGPhotoEnhanceHistogramBins, sizeof(uint32_t));
        cdfs[a] = calloc(PGPhotoEnhanceHistogramBins, sizeof(uint32_t));
    }

    float xMul = PGPhotoEnhanceSegments / imageWidth;
    float yMul = PGPhotoEnhanceSegments / imageHeight;

    for (uint32_t y = 0; y < imageHeight; y++) {
        uint32_t yOffset = y * width * 4;
        for (uint32_t x = 0; x < imageWidth; x++) {
            uint32_t index = x * 4 + yOffset;

            uint32_t tx = (uint32_t) (x * xMul);
            uint32_t ty = (uint32_t) (y * yMul);
            uint32_t t = ty * PGPhotoEnhanceSegments + tx;

            hist[t][bytes[index + 2]]++;
        }
    }

    for (uint32_t i = 0; i < totalSegments; i++) {
        if (clipLimit > 0) {
            uint32_t clipped = 0;
            for (uint32_t j = 0; j < PGPhotoEnhanceHistogramBins; ++j) {
                if (hist[i][j] > clipLimit) {
                    clipped += hist[i][j] - clipLimit;
                    hist[i][j] = clipLimit;
                }
            }

            uint32_t redistBatch = clipped / PGPhotoEnhanceHistogramBins;
            uint32_t residual = clipped - redistBatch * PGPhotoEnhanceHistogramBins;

            for (uint32_t j = 0; j < PGPhotoEnhanceHistogramBins; ++j) {
                hist[i][j] += redistBatch;
            }

            for (uint32_t j = 0; j < residual; ++j) {
                hist[i][j]++;
            }
        }
        memcpy(cdfs[i], hist[i], PGPhotoEnhanceHistogramBins * sizeof(uint32_t));

        uint32_t hMin = PGPhotoEnhanceHistogramBins - 1;
        for (uint32_t j = 0; j < hMin; ++j) {
            if (cdfs[j] != 0) {
                hMin = j;
            }
        }

        uint32_t cdf = 0;
        for (uint32_t j = hMin; j < PGPhotoEnhanceHistogramBins; ++j) {
            cdf += cdfs[i][j];
            cdfs[i][j] = (uint8_t) MIN(255, cdf * scale);
        }

        cdfsMin[i] = cdfs[i][hMin];
        cdfsMax[i] = cdfs[i][PGPhotoEnhanceHistogramBins - 1];
    }

    uint32_t resultSize = 4 * PGPhotoEnhanceHistogramBins * totalSegments;
    uint32_t resultBytesPerRow = 4 * PGPhotoEnhanceHistogramBins;

    unsigned char *result = (*env)->GetDirectBufferAddress(env, buffer);
    for (uint32_t tile = 0; tile < totalSegments; tile++) {
        uint32_t yOffset = tile * resultBytesPerRow;
        for (uint32_t i = 0; i < PGPhotoEnhanceHistogramBins; i++) {
            uint32_t index = i * 4 + yOffset;
            result[index] = (uint8_t) cdfs[tile][i];
            result[index + 1] = (uint8_t) cdfsMin[tile];
            result[index + 2] = (uint8_t) cdfsMax[tile];
            result[index + 3] = 255;
        }
    }

    for (uint32_t a = 0; a < totalSegments; a++) {
        free(hist[a]);
        free(cdfs[a]);
    }
    free(hist);
    free(cdfs);
    free(cdfsMax);
    free(cdfsMin);
}


