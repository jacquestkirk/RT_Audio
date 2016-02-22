%% setup
%sample_rate = 44100*2;
%bit_rate = 1000;

sample_rate = 44100;
f_center_hz = sample_rate/4;
bit_rate = 1000;
f_dev = bit_rate/2;
bw_carson = bit_rate + 2*f_dev;

noise_pwr = 0.15;

iq_filt_len = 30;
f_pass_hz = bw_carson/2;
f_stop_hz = 2*f_center_hz - bw_carson/2;
f_pass_norm = max(0.2, f_pass_hz/(sample_rate/2));
f_stop_norm = min(0.6, f_stop_hz/(sample_rate/2));
df_trans_hz = f_stop_hz - f_pass_hz;

rx_filt_len = 30;
f_pass_lo_hz = f_center_hz - bw_carson/2;
f_pass_hi_hz = f_center_hz + bw_carson/2;
f_stop_lo_hz = f_pass_lo_hz - df_trans_hz; %rx filter should have same transition band as iq filter to prevent noise "aliasing" due to sum of frequency demod component
f_stop_hi_hz = f_pass_hi_hz + df_trans_hz;
f_pass_lo_norm = f_pass_lo_hz/(sample_rate/2);
f_pass_hi_norm = f_pass_hi_hz/(sample_rate/2);
f_stop_lo_norm = f_stop_lo_hz/(sample_rate/2);
f_stop_hi_norm = f_stop_hi_hz/(sample_rate/2);


mesg = [170 170 66 76 79 87 77 69];
mesg = repmat(mesg,1,100);
%t_s = 0:(1/sample_rate):(0.064-1/sample_rate);
t_s = [1:length(mesg)*8*sample_rate/bit_rate-1]*1/sample_rate;
t_s = [1:length(data)]*1/sample_rate;
mesg_bits = dec2bin(mesg);
mesg_bits_T = transpose(mesg_bits);
sig_sample = zeros(size(t_s));
bit_sample = zeros(size(t_s));
f1_hz = f_center_hz - f_dev;
f2_hz = f_center_hz + f_dev;


%% create transmit signal
if(false)
    
    for i = (1:length(t_s))
      if mesg_bits_T(round(i/(sample_rate/bit_rate)+0.5)) == '0'
        sig_sample(i) = sin(2*pi*f1_hz*t_s(i));
        bit_sample(i) = 0;
      elseif mesg_bits_T(round(i/(sample_rate/bit_rate)+0.5)) == '1'
        sig_sample(i) = sin(2*pi*f2_hz*t_s(i));
        bit_sample (i) = 1;
      end
    end 


    sig_sample = sig_sample + noise_pwr*randn(size(t_s));
end
sig_sample = data';

%% Create and apply rx filter
% figure(3)
% subplot(2,1,1)
% filt_f = [0,f_stop_lo_norm, f_pass_lo_norm, f_pass_hi_norm,f_stop_hi_norm,1];
% filt_a = [0,0,1,1,0,0];
% filt_rx_h = firpm(iq_filt_len,filt_f, filt_a);
% plot(20*log10(abs(fft(filt_rx))))
% sig_sample_filt = filter(filt_rx_hz, 1, sig_sample);
%We can actually ignore this since we're pretty much using the entire
%spectrum. 
sig_sample_filt = sig_sample;

%% Generate LO

cw_sin = -sin(2*pi*f_center_hz*t_s);
cw_cos = cos(2*pi*f_center_hz*t_s);


%% Demodulate
%sig_sample = sin(2*pi*f_center_hz*t_s);

i_dat = sig_sample_filt.*cw_cos;
q_dat = sig_sample_filt.*cw_sin;


%i_dat = cw_cos;
%q_dat = cw_sin;

%% Create and apply IQ filter
figure(3)
subplot(2,1,2)
filt_f = [0,f_pass_norm,f_stop_norm,1];
filt_a = [1,1,0,0];
filt_h = firpm(iq_filt_len,filt_f, filt_a);
plot(20*log10(abs(fft(filt_h))))

i_dat_filt = filter(filt_h, 1, [i_dat,zeros(1,iq_filt_len/2)]);
q_dat_filt = filter(filt_h, 1, [q_dat,zeros(1,iq_filt_len/2)]);

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
%f_dm = fmdemod(sig_sample, f_center_hz, sample_rate, f_dev);
freq_dat = diff(unwrapped_phase)*sample_rate/(2*pi);
freq_dat = conv(freq_dat,ones(1,round(sample_rate/bit_rate))*1/round(sample_rate/bit_rate));

%% detection

osr = sample_rate/bit_rate ;
initial_delay = (iq_filt_len/2) + osr/2 ;


rx_bits = zeros(1,length(mesg_bits_T(:)));
for i = 1: length(rx_bits)
    next_sample = round(initial_delay + osr*(i-1));
    rx_bits(i) = (freq_dat(next_sample)/f_dev) > 0;
end


%% calculate BER
errors = sum(str2num(mesg_bits_T(:)) ~= rx_bits');
BER = errors/length(rx_bits)

%% plot time domain
figure(1)
subplot(3,1,1);
plot((1:length(freq_dat))-initial_delay,freq_dat/f_dev,'g')
%plot(q_dat,'g')
%plot(f_dm, 'b')
hold on
%plot (i_dat,'b')
plot(2*(bit_sample-0.5),'c')
%plot(unwrapped_phase/pi, 'r')
xlim([0,length(bit_sample)])
hold off

subplot(3,1,2)
plot(str2num(mesg_bits_T(:)))

subplot(3,1,3)
plot(rx_bits)



%ylim([-300,300])




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