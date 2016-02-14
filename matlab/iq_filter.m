sample_rate = 44100;
f_center_hz = sample_rate/4
bit_rate = 9000
f_dev = bit_rate/2
bw_carson = bit_rate + 2*f_dev;
f_pass_hz = bw_carson/2
f_stop_hz = 2*f_center_hz - bw_carson/2

f_pass_norm = f_pass_hz/sample_rate *2
f_stop_norm = f_stop_hz/sample_rate *2

