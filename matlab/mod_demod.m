%% setup
sample_rate = 40000*2;
bit_rate = 1000;

mesg = [170 170 66 76 79 87 77 69];
%t_s = 0:(1/sample_rate):(0.064-1/sample_rate);
t_s = [1:length(mesg)*8*sample_rate/bit_rate-1]*1/sample_rate;
mesg_bits = dec2bin(mesg);
mesg_bits_T = transpose(mesg_bits);
sig_sample = zeros(size(t_s));
bit_sample = zeros(size(t_s));
f1_hz = 8000;
f2_hz = 12000;

%calculations
f_center_hz = mean([f1_hz,f2_hz]);
f_dev = (f2_hz-f1_hz)/2;

%% create transmit signal
for i = (1:length(t_s))
  if mesg_bits_T(round(i/(sample_rate/bit_rate)+0.5)) == '0'
    sig_sample(i) = sin(2*pi*f1_hz*t_s(i));
    bit_sample(i) = 0;
  elseif mesg_bits_T(round(i/(sample_rate/bit_rate)+0.5)) == '1'
    sig_sample(i) = sin(2*pi*f2_hz*t_s(i));
    bit_sample (i) = 1;
  end
end 


%sig_sample = sig_sample + 0.3*randn(size(t_s));

%% Generate LO

cw_sin = -sin(2*pi*f_center_hz*t_s);
cw_cos = cos(2*pi*f_center_hz*t_s);


%% Demodulate
%sig_sample = sin(2*pi*f_center_hz*t_s);

i_dat = sig_sample.*cw_cos;
q_dat = sig_sample.*cw_sin;


%i_dat = cw_cos;
%q_dat = cw_sin;

%% Create and apply filter
figure(3)
filt_f = [0,0.25,0.4,1];
filt_a = [1,1,0,0];
filt_h = firpm(30,filt_f, filt_a);
plot(20*log10(abs(fft(filt_h))))

i_dat_filt = filter(filt_h, 1, i_dat);
q_dat_filt = filter(filt_h, 1, q_dat);

%% pull frequency out of IQ data
phase = atan(q_dat_filt./i_dat_filt);


phase2 = phase;
for i = 1: length(t_s)
    if i_dat_filt(i) < 0
        phase2(i) = phase(i) + pi;
    end
end

phase2= atan2(q_dat_filt,i_dat_filt);

unwrapped_phase = unwrap(phase2);

%unwrapped_phase = conv(unwrapped_phase,ones(1,sample_rate/bit_rate));
f_dm = fmdemod(sig_sample, f_center_hz, sample_rate, f_dev);
freq_dat = diff(unwrapped_phase)*sample_rate/(2*pi);
%freq_dat = conv(freq_dat,ones(1,sample_rate/bit_rate));

figure(1)
plot(freq_dat/f_dev,'g')
%plot(q_dat,'g')
%plot(f_dm, 'b')
hold on
%plot (i_dat,'b')
plot(1*(bit_sample-0.5),'c')
%plot(unwrapped_phase/pi, 'r')

hold off
%ylim([-300,300])
xlim([0,1500])



%% Take a look at frequency domain

%plot(sig_sample(1:length(t_s)))

figure(2)
subplot(3,1,1)
fft_sig = fft(sig_sample,length(t_s));
P_fft_sig = 10*log10(fft_sig.*conj(fft_sig)/length(t_s));
f = sample_rate/length(t_s)*(0:((length(t_s)/2)-1))/1000;
plot(f,P_fft_sig(1:(length(t_s)/2)))
title('Power spectral density')
xlabel('Frequency (kHz)')
ylim([-100,50])

subplot(3,1,2)
fft_i = fft(i_dat_filt,length(t_s));
P_fft_i = 10*log10(fft_i.*conj(fft_i)/length(t_s));
f = sample_rate/length(t_s)*(0:((length(t_s)/2)-1))/1000;
plot(f,P_fft_i(1:(length(t_s)/2)))
title('Power spectral density')
xlabel('Frequency (kHz)')
ylim([-100,50])

subplot(3,1,3)
fft_q = fft(q_dat_filt,length(t_s));
P_fft_q = 10*log10(fft_q.*conj(fft_q)/length(t_s));
f = sample_rate/length(t_s)*(0:((length(t_s)/2)-1))/1000;
plot(f,P_fft_q(1:(length(t_s)/2)))
title('Power spectral density')
xlabel('Frequency (kHz)')
ylim([-100,50])

%plot(y(1:50))
%title('Time domain signal')

%X = fft(x,251);
%Pxx = X.*conj(X)/251;
%f = 1000/251*(0:127)/1000;
%plot(f,Pxx(1:128))
%title('Power spectral density')
%xlabel('Frequency (kHz)')