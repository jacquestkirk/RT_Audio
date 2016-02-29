freq_dat_phone = load('Freq.csv');
freq_filt_dat_phone = load('FreqFilt.csv');
figure(6)
plot(freq_filt_dat_phone/f_dev)
hold on
%plot(freq_dat_phone/f_dev,'g')
plot(2*(bit_sample),'c')
%plot((1:length(freq_dat))-initial_delay,freq_dat/f_dev,'g')
%plot(phase2,'r')