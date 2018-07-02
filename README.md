# Trích xuất đặc trưng mfcc của đoạn âm thanh.
    - MFCC được trích xuất trực tiếp từ recorder(trong quá trình ghi âm).
    Danh sách tham số:
    
            .sampleRate: sample rate của đoạn ghi âm setup trong recorder
            .audioBufferSize: buffersize của đoạn ghi âm setup trong recorder
            .sampleSizeInBits: Bit per second, số bit trên 1 sample được setup trong recorder
            .bufferOverlap: số tín hiệu chồng lên nhau(frameshift).
            .recorder: recorder do mình tạo ra làm nhiệm vụ ghi âm.
            .channel: số kênh (mono(1), sereo(2))
            .bufferCallback: callback mỗi lần lấy mẫu để ghi tín hiệu xuống file.
    

            dispatcher = AudioDispatcherFactory.fromMic(RECORDER_SAMPLERATE, RECORDER_BUFFER_SIZE, RECORDER_BPP,
                    0, mRecorder.getRecorder(), 2, mRecorder);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    ExtractMFCCsTask task = new ExtractMFCCsTask(RealtimeMfccActivity.this, RECORDER_SAMPLERATE);
                    task.execute(audioEvent);
                    return true;
                }

              @Override
              public void processingFinished() {
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          mtvInfo.setText("processingFinished: " + vectors.size());
                      }
                  });
              }
            });
            dispatcher.run();

   
# Tính độ chênh lệch giữa 2 đoạn âm thanh(sử dụng kết quả mfcc).
        
            //Đọc 2 file có chứa dữ liệu trích xuất mfcc
            List<double[]> result = FileHelper.ReadFile("data.txt", 39);
            List<double[]> des = FileHelper.ReadFile("mfcc.txt", 39);
            //tính khoảng chênh lệch mfcc giữa 2 tín hiệu đọc được từ file
            DTW dtw = DTW.getInstance();
            double distance = dtw.process(result, des).getDistance();
