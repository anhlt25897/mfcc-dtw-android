# 1. Trích xuất đặc trưng mfcc của đoạn âm thanh.
MFCC được trích xuất trực tiếp từ recorder(trong quá trình ghi âm). 

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

# Trích xuất mfcc.

          .audioBufferSize: buffersize của đoạn ghi âm setup trong recorder
          .sampleRate: sample rate của đoạn ghi âm setup trong recorder
          .amountOfCepstrumCoef: Số đặc trưng mfcc muốn lấy.
          .amountOfMelFilters: Số băng lọc mel
          .lowerFilterFreq: Tần số nhỏ nhất giọng nói của người(thường là 300)
          .upperFilterFreq: Tần số lớn nhất giọng nói của người(thường là 8000)
          
          MFCC mfcc = new MFCC(a[0].getBufferSize(), mSampleRate, 39, 40, 300, 8000);
          boolean isSuccess = mfcc.process(a[0]);
   
# 2. Tính độ chênh lệch giữa 2 đoạn âm thanh(sử dụng kết quả mfcc).
        
            //Đọc 2 file có chứa dữ liệu trích xuất mfcc
            List<double[]> result = FileHelper.ReadFile("data.txt", 39);
            List<double[]> des = FileHelper.ReadFile("mfcc.txt", 39);
            //tính khoảng chênh lệch mfcc giữa 2 tín hiệu đọc được từ file
            DTW dtw = DTW.getInstance();
            double distance = dtw.process(result, des).getDistance();

# 3. Thay đổi trong thư viện dsp.(Giải quyết vấn đề: thư viện dsp không lưu được file khi trích xuất mfcc realtime)
            while (!stopped && !endOfStream && totalBytesRead < bytesToRead) {
                try {
                    bytesRead = audioInputStream.read(audioByteBuffer, offsetInBytes + totalBytesRead, bytesToRead - totalBytesRead);
                    //gọi ra ngoài để recorder có thể ghi xuống file.
               --->     mBufferCallback.writeBuffer(audioByteBuffer);
                } catch (IndexOutOfBoundsException e) {
                    // The pipe decoder generates an out of bounds if end
                    // of stream is reached. Ugly hack...
                    bytesRead = -1;
                }
                if (bytesRead == -1) {
                    // The end of the stream is reached if the number of bytes read during this iteration equals -1
                    endOfStream = true;
                } else {
                    // Otherwise add the number of bytes read to the total
                    totalBytesRead += bytesRead;
                }
            }
            
            //báo ra ngoài khi kết thúc việc lấy mẫu để phân tích(cũng là lúc ngừng ghi âm)
            ---> mBufferCallback.onDone();
