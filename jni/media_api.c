/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <android/bitmap.h>

#include "libavutil/pixfmt.h"
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"

#include "debug.h"

#define true 1
#define false 0

AVFormatContext *gAVFormatContext;
AVStream *gAudioAVStream;
int gAudioStreamIndex;
int gVideoFrameNumber;

jbyte* g_buffer;
struct SwrContext *gSwrContext;
void* buffer;
AVFrame *gRGBAFrame = NULL;
AVFrame *gYUVFrame = NULL;

//            out_sample_fmt = AV_SAMPLE_FMT_S16;
//static const SampleFmtInfo sample_fmt_info[AV_SAMPLE_FMT_NB] = {
//    [AV_SAMPLE_FMT_U8]   = { .name =   "u8", .bits =  8, .planar = 0, .altform = AV_SAMPLE_FMT_U8P  },
//    [AV_SAMPLE_FMT_S16]  = { .name =  "s16", .bits = 16, .planar = 0, .altform = AV_SAMPLE_FMT_S16P },
//    [AV_SAMPLE_FMT_S32]  = { .name =  "s32", .bits = 32, .planar = 0, .altform = AV_SAMPLE_FMT_S32P },
//    [AV_SAMPLE_FMT_FLT]  = { .name =  "flt", .bits = 32, .planar = 0, .altform = AV_SAMPLE_FMT_FLTP },
//    [AV_SAMPLE_FMT_DBL]  = { .name =  "dbl", .bits = 64, .planar = 0, .altform = AV_SAMPLE_FMT_DBLP },
//    [AV_SAMPLE_FMT_U8P]  = { .name =  "u8p", .bits =  8, .planar = 1, .altform = AV_SAMPLE_FMT_U8   },
//    [AV_SAMPLE_FMT_S16P] = { .name = "s16p", .bits = 16, .planar = 1, .altform = AV_SAMPLE_FMT_S16  },
//    [AV_SAMPLE_FMT_S32P] = { .name = "s32p", .bits = 32, .planar = 1, .altform = AV_SAMPLE_FMT_S32  },
//    [AV_SAMPLE_FMT_FLTP] = { .name = "fltp", .bits = 32, .planar = 1, .altform = AV_SAMPLE_FMT_FLT  },
//    [AV_SAMPLE_FMT_DBLP] = { .name = "dblp", .bits = 64, .planar = 1, .altform = AV_SAMPLE_FMT_DBL  },
//};

void ff_log_callback(void *ptr, int level, const char *fmt, va_list vargs)
{
    LOGV(fmt, vargs);
}

    jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativeMediaInit(JNIEnv* env, jobject thiz, jstring pFilename)
{
    char *videoFileName = (char *)(*env)->GetStringUTFChars(env, pFilename, NULL);
    LOGI("Video file name is %s", videoFileName);

    av_register_all();
    av_log_set_callback(ff_log_callback);
    av_log_set_level(AV_LOG_VERBOSE);

    // open the video file
    if (avformat_open_input(&gAVFormatContext, videoFileName, NULL, NULL) != 0) {
        LOGE("could not open video file: %s", videoFileName);
        return -1;
    }

    LOGI("After avformat_open, video file name is %s", videoFileName);
    // retrieve stream info
    if (avformat_find_stream_info(gAVFormatContext, NULL) < 0) {
        LOGE("could not find stream info");
        return -1;
    }

    int index;
    gAudioStreamIndex = -1;
    LOGI("Stream no:%d", gAVFormatContext->nb_streams);
    for (index = 0; index < gAVFormatContext->nb_streams; index++) {
        if (AVMEDIA_TYPE_AUDIO == gAVFormatContext->streams[index]->codec->codec_type) {
            gAudioStreamIndex = index;
            gAudioAVStream = gAVFormatContext->streams[index];
        }
    }

    if (-1 == gAudioStreamIndex) {
        LOGI("could not find a video stream");
        return -1;
    }

    AVCodecContext *pCodecctx = gAVFormatContext->streams[gAudioStreamIndex]->codec;
    AVCodec *pCodec = avcodec_find_decoder(pCodecctx->codec_id);
    LOGI("AVCodec name: %s", pCodec->name);
    LOGI("AVCodec long name: %s", pCodec->long_name);
    if (pCodec == NULL) {
        LOGE("unsupported codec");
        return -1;
    }

    if (avcodec_open2(pCodecctx, pCodec, NULL) < 0) {
        LOGE("could not open codec");
        return -1;
    }

    gVideoFrameNumber = 0;
    LOGI("After avcodec_open2");

    return 0;
}

jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativeGetSampleRate(JNIEnv* env, jobject thiz) {
    if (NULL != gAudioAVStream) {
        return (gAudioAVStream->codec->sample_rate);
    } else {
        return -1;
    }
}

jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativeGetChannelCount(JNIEnv* env, jobject thiz) {
    if (NULL != gAudioAVStream) {
        return gAudioAVStream->codec->channels;
    } else {
        return 1;
    }
}

jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativePrepareByteBuffer(JNIEnv* env,
        jobject thiz, jobject pByteBuffer) {
    LOGI("nativePrepareByteBuffer");
    if (pByteBuffer == NULL) {
        return false;
    }

    g_buffer = (jbyte*)(*env)->GetDirectBufferAddress(env, pByteBuffer);

    if (g_buffer == NULL) {
        return false;
    }

    gSwrContext = swr_alloc_set_opts(gSwrContext,
            gAudioAVStream->codec->channel_layout, AV_SAMPLE_FMT_S16, gAudioAVStream->codec->sample_rate,
            gAudioAVStream->codec->channel_layout, gAudioAVStream->codec->sample_fmt, gAudioAVStream->codec->sample_rate,
            0, 0);
    //
    //    gSwsContext = swr_alloc();
    //    av_opt_set_int(gSwsContext, "in_channel_layout",  gAudioAVStream->codec->channel_layout, 0);
    //    av_opt_set_int(gSwsContext, "out_channel_layout", gAudioAVStream->codec->channel_layout,  0);
    //    av_opt_set_int(gSwsContext, "in_sample_rate",     gAudioAVStream->codec->sample_rate, 0);
    //    av_opt_set_int(gSwsContext, "out_sample_rate",    gAudioAVStream->codec->sample_rate, 0);
    //    av_opt_set_sample_fmt(gSwsContext, "in_sample_fmt",  gAudioAVStream->codec->sample_fmt, 0);
    //    av_opt_set_sample_fmt(gSwsContext, "out_sample_fmt", AV_SAMPLE_FMT_S16,  0);
    return 0;
}


    jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativeGetFrame(JNIEnv* env, jobject thiz)
{
    AVPacket packet;
    int got_frame;
    AVFrame *decoded_frame = NULL;
    //    "([B)V"
    //    jmethodID putMethod = (*env)->GetMethodID(_javaScClass,"Putarray", "([B)V");
    jclass cls = (*env)->GetObjectClass(env, thiz);
    jmethodID putMethod = (*env)->GetMethodID(env, cls, "Putarray", "([B)V");

    if (gYUVFrame == NULL) {
        LOGE("jason oops, gYUVFrame is NULL");
    }

    LOGE("Audio format:%d", gAudioAVStream->codec->sample_fmt);
    LOGE("Audio sample_rate:%d", gAudioAVStream->codec->sample_rate);
    LOGE("Audio channels:%d", gAudioAVStream->codec->channels);
    while (av_read_frame(gAVFormatContext, &packet) >= 0) {
        LOGE("jason packet size:%d", packet.size);
        LOGE("jason packet first data:%d", packet.data[0]);
        LOGE("jason packet 2nd data:%d", packet.data[1]);
        LOGE("jason packet 3rd data:%d", packet.data[2]);
        LOGE("jason packet end data:%d", packet.data[packet.size - 1]);
        LOGE("jason av_read_frame, gAudioStreamIndex:%d", gAudioStreamIndex);
        LOGE("jason av_read_frame, packet.stream_index:%d", packet.stream_index);
        if (gAudioStreamIndex == packet.stream_index) {
            if (!decoded_frame) {
                if (!(decoded_frame = avcodec_alloc_frame())) {
                    LOGE("Could not allocate audio frame\n");
                    exit(1);
                }
            }
            avcodec_get_frame_defaults(decoded_frame);

            LOGE("Got audio channels:%d", gAudioAVStream->codec->channels);
            LOGE("Got audio frame bit_rate:%d", gAudioAVStream->codec->bit_rate);
            LOGE("Got audio frame bits_per_coded_sample:%d", gAudioAVStream->codec->bits_per_coded_sample);
            // Actually decode video API
            //            avcodec_decode_audio4(gAudioAVStream->codec, gYUVFrame, &got_frame, &packet);
            //            avcodec_decode_video2(gAudioAVStream->codec, gYUVFrame, &got_frame, &packet);
            int len = avcodec_decode_audio4(gAudioAVStream->codec, decoded_frame, &got_frame, &packet);
            LOGE("Got audio frame len:%d", len);
            LOGE("Got audio frame sample_rate:%d", decoded_frame->sample_rate);
            LOGE("Got audio frame pts:%ld", decoded_frame->pts);
            LOGE("Got audio frame pkt_pts:%ld", decoded_frame->pkt_pts);
            LOGE("Got audio frame pkt_dts:%ld", decoded_frame->pkt_dts);
            int data_size = av_samples_get_buffer_size(NULL, gAudioAVStream->codec->channels,
                    decoded_frame->nb_samples,
                    gAudioAVStream->codec->sample_fmt, 0);

            LOGE("Got audio frame data_size:%d", data_size);
            LOGE("Got audio frame nb_samples:%d", decoded_frame->nb_samples);
            LOGE("Got audio frame sample_fmt:%d", gAudioAVStream->codec->sample_fmt);
            uint8_t pTemp[data_size];
            uint8_t *pOut = (uint8_t *)&pTemp;
            swr_init(gSwrContext);
            swr_convert(gSwrContext, (uint8_t **)(&pOut), decoded_frame->nb_samples,
                    (const uint8_t **)decoded_frame->extended_data,
                    decoded_frame->nb_samples);

            data_size = av_samples_get_buffer_size(NULL, gAudioAVStream->codec->channels,
                    decoded_frame->nb_samples,
                    AV_SAMPLE_FMT_S16, 0);
            memcpy(g_buffer, pOut, data_size);
            LOGV("jason copy data_size:%d to g j buffer:", data_size);
            gVideoFrameNumber++;
            return data_size;
        }

        av_free_packet(&packet);
    }

    LOGE("end:%d", gVideoFrameNumber);
    return -1;
}

    jint
Java_com_jasonsoft_softwareaudioplayer_VideoSurfaceView_nativeMediaFinish(JNIEnv* env, jobject thiz, jobject pBitmap)
{
    AndroidBitmap_unlockPixels(env, pBitmap);
    // Free the RGB image
    av_free(gRGBAFrame);
    // Free the YUV frame
    av_free(gYUVFrame);

    avcodec_close(gAudioAVStream->codec);
    avformat_close_input(&gAVFormatContext);
    return 0;
}

