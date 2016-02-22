Fs = 44100;
nbits = 16;
dev_id = getfield(getfield(audiodevinfo, 'input'), 'ID');

arec = audiorecorder(Fs, nbits, 1, dev_id);
record(arec);
pause(10);
stop(arec);

data = getaudiodata(arec);

plot(data)