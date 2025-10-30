whisper_init_from_file_with_params_no_state: loading model from '/opt/homebrew/Cellar/whisper-cpp/1.8.2/share/whisper-cpp/models/ggml-medium.bin'
whisper_init_with_params_no_state: use gpu    = 1
whisper_init_with_params_no_state: flash attn = 1
whisper_init_with_params_no_state: gpu_device = 0
whisper_init_with_params_no_state: dtw        = 0
ggml_metal_library_init: using embedded metal library
ggml_metal_library_init: loaded in 0.026 sec
ggml_metal_device_init: GPU name:   Apple M3
ggml_metal_device_init: GPU family: MTLGPUFamilyApple9  (1009)
ggml_metal_device_init: GPU family: MTLGPUFamilyCommon3 (3003)
ggml_metal_device_init: GPU family: MTLGPUFamilyMetal4  (5002)
ggml_metal_device_init: simdgroup reduction   = true
ggml_metal_device_init: simdgroup matrix mul. = true
ggml_metal_device_init: has unified memory    = true
ggml_metal_device_init: has bfloat            = true
ggml_metal_device_init: use residency sets    = true
ggml_metal_device_init: use shared buffers    = true
ggml_metal_device_init: recommendedMaxWorkingSetSize  = 12713.12 MB
whisper_init_with_params_no_state: devices    = 3
whisper_init_with_params_no_state: backends   = 3
whisper_model_load: loading model
whisper_model_load: n_vocab       = 51865
whisper_model_load: n_audio_ctx   = 1500
whisper_model_load: n_audio_state = 1024
whisper_model_load: n_audio_head  = 16
whisper_model_load: n_audio_layer = 24
whisper_model_load: n_text_ctx    = 448
whisper_model_load: n_text_state  = 1024
whisper_model_load: n_text_head   = 16
whisper_model_load: n_text_layer  = 24
whisper_model_load: n_mels        = 80
whisper_model_load: ftype         = 1
whisper_model_load: qntvr         = 0
whisper_model_load: type          = 4 (medium)
whisper_model_load: adding 1608 extra tokens
whisper_model_load: n_langs       = 99
whisper_model_load:        Metal total size =  1533.14 MB
whisper_model_load: model size    = 1533.14 MB
whisper_backend_init_gpu: using Metal backend
ggml_metal_init: allocating
ggml_metal_init: found device: Apple M3
ggml_metal_init: picking default device: Apple M3
ggml_metal_init: use bfloat         = true
ggml_metal_init: use fusion         = true
ggml_metal_init: use concurrency    = true
ggml_metal_init: use graph optimize = true
whisper_backend_init: using BLAS backend
whisper_init_state: kv self size  =   50.33 MB
whisper_init_state: kv cross size =  150.99 MB
whisper_init_state: kv pad  size  =    6.29 MB
whisper_init_state: compute buffer (conv)   =   29.53 MB
whisper_init_state: compute buffer (encode) =   44.60 MB
whisper_init_state: compute buffer (cross)  =   20.02 MB
whisper_init_state: compute buffer (decode) =   99.12 MB
error: failed to read audio data as wav (Unknown error)
error: failed to read audio file 'temp/audio_b7814a73.m4a'

whisper_print_timings:     load time =  1512.04 ms
whisper_print_timings:     fallbacks =   0 p /   0 h
whisper_print_timings:      mel time =     0.00 ms
whisper_print_timings:   sample time =     0.00 ms /     1 runs (     0.00 ms per run)
whisper_print_timings:   encode time =     0.00 ms /     1 runs (     0.00 ms per run)
whisper_print_timings:   decode time =     0.00 ms /     1 runs (     0.00 ms per run)
whisper_print_timings:   batchd time =     0.00 ms /     1 runs (     0.00 ms per run)
whisper_print_timings:   prompt time =     0.00 ms /     1 runs (     0.00 ms per run)
whisper_print_timings:    total time =  1609.99 ms
ggml_metal_free: deallocating

source: https://www.bilibili.com/video/BV1CPsZeXEr6/?spm_id_from=333.1387.homepage.video_card.click&vd_source=2b20beae90a6e698bc74a01d0df596e2
