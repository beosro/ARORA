import socket
import sys
import atexit
import RPi.GPIO as GPIO
import time
import threading

from Raspi_MotorHAT import Raspi_MotorHAT, Raspi_DCMotor
from Raspi_PWM_Servo_Driver import PWM
from ar_markers import detect_markers

try:
    import cv2
except ImportError:
    raise Exception('Error: OpenCv is not installed')

udp_serial_no_temp = 0

message_ok = False

UDP_IP = "192.168.0.16"
# UDP_IP = "10.0.0.5"
UDP_PORT = 5555
data_rx = ""
data_tx = ""
data_rx_temp = ""
header_temp = ""

header = ""
udp_serial_no = 0
cmd_mode = 0
control_mode = 0
set_speed = 0
car_fwd_back = 0
car_left_right = 0
cam_up_down = 0
cam_left_right = 0

servo_min = 150
servo_mid = 345
servo_max = 550

marker_id = 0


def get_rx_message():
    global data_rx_temp
    data_rx_temp_lock = threading.Lock()

    global UDP_IP, UDP_PORT
    # Create a UDP socket
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        print('Socket created')
    except socket.error as msg:
        print('Failed to create socket. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
        sys.exit()

    # Bind the socket to the port
    server_address = (UDP_IP, UDP_PORT)
    print >> sys.stderr, 'starting up on %s port %s' % server_address
    try:
        sock.bind(server_address)
    except socket.error as msg:
        print('Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
        sys.exit()

    print('Socket bind complete')
    atexit.register(sock.close)

    while True:
        print >> sys.stderr, '\nwaiting to receive message'
        with data_rx_temp_lock:
            data_rx_temp, address = sock.recvfrom(4096)
        do_data_rx_decode()


def do_data_rx_decode():
    global data_rx, data_rx_temp, header_temp
    global header, udp_serial_no, cmd_mode, control_mode
    global set_speed, car_fwd_back, car_left_right, cam_up_down, cam_left_right
    messages_rx = data_rx_temp.split(",")
    header_temp = messages_rx[0]
    print("Header:" + header_temp)
    udp_serial_no_temp2 = int(messages_rx[1])
    print("UDP No: " + str(udp_serial_no_temp2))
    cmd_mode_temp = int(messages_rx[2])
    control_mode_temp = int(messages_rx[3])
    set_speed_temp = int(messages_rx[4])
    car_fwd_back_temp = int(messages_rx[5])
    car_left_right_temp = int(messages_rx[6])
    cam_up_down_temp = int(messages_rx[7])
    cam_left_right_temp = int(messages_rx[8])
    print("Data Decoded")

    check_header()

    if message_ok:
        # Discarding packets with header mismatch
        print("Header checked : OK")

        data_rx_lock = threading.Lock()
        with data_rx_lock:
            data_rx = data_rx_temp
            header = header_temp
            udp_serial_no = udp_serial_no_temp2
            cmd_mode = cmd_mode_temp
            control_mode = control_mode_temp
            set_speed = set_speed_temp
            car_fwd_back = car_fwd_back_temp
            car_left_right = car_left_right_temp
            cam_up_down = cam_up_down_temp
            cam_left_right = cam_left_right_temp
            print("Message: " + data_rx)

    else:
        print("Header checked : Fail")
        pass


def check_header():
    global header_temp, message_ok, data_rx, data_rx_temp

    if header_temp == "ARORA":
        message_ok = True
    else:
        message_ok = False


def do_control():
    global car_fwd_back, car_left_right, set_speed
    atexit.register(turnOffMotors)
    atexit.register(reset_all_servos)
    while True:
        global cmd_mode, control_mode

        if cmd_mode == 0 and control_mode == 0:  # Manual Cam
            do_cam_control()

        elif cmd_mode == 0 and control_mode == 1:  # Manual Car
            do_car_control(car_fwd_back, car_left_right, set_speed)

        elif cmd_mode == 1 and control_mode == 0:  # Manual Cam + Auto OFF
            do_cam_control()

        elif cmd_mode == 1 and control_mode == 1:  # Manual Cam + Auto ON
            pass


def do_car_control(car_fwd_back_cntrl, car_left_right_cntrl, set_speed_cntrl):
    global set_speed, motor_fwd_back, motor_fwd_back_2, motor_left_right

    motor_fwd_back.setSpeed(set_speed_cntrl)
    motor_fwd_back_2.setSpeed(set_speed_cntrl)
    motor_left_right.setSpeed(set_speed_cntrl)

    if car_fwd_back_cntrl == 0:
        motor_fwd_back.run(Raspi_MotorHAT.RELEASE)
        motor_fwd_back_2.run(Raspi_MotorHAT.RELEASE)
        motor_left_right.run(Raspi_MotorHAT.RELEASE)
        print("\nCar Stopped")

        if car_left_right_cntrl == -1:
            motor_left_right.run(Raspi_MotorHAT.BACKWARD)
            print("\nCar Look Left")
        elif car_left_right_cntrl == 0:
            motor_left_right.run(Raspi_MotorHAT.RELEASE)
        elif car_left_right_cntrl == 1:
            motor_left_right.run(Raspi_MotorHAT.FORWARD)
            print("\nCar Look Right")

    elif car_fwd_back_cntrl == -1:
        if car_left_right_cntrl == -1:
            motor_fwd_back.run(Raspi_MotorHAT.BACKWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.BACKWARD)
            motor_left_right.run(Raspi_MotorHAT.BACKWARD)
            print("\nCar Back Left")
        elif car_left_right_cntrl == 0:
            motor_fwd_back.run(Raspi_MotorHAT.BACKWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.BACKWARD)
            motor_left_right.run(Raspi_MotorHAT.RELEASE)
            print("\nCar Back")
        elif car_left_right_cntrl == 1:
            motor_fwd_back.run(Raspi_MotorHAT.BACKWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.BACKWARD)
            motor_left_right.run(Raspi_MotorHAT.FORWARD)
            print("\nCar Back Right")

    elif car_fwd_back_cntrl == 1:
        if car_left_right_cntrl == -1:
            motor_fwd_back.run(Raspi_MotorHAT.FORWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.FORWARD)
            motor_left_right.run(Raspi_MotorHAT.BACKWARD)
            print("\nCar Forward Left")
        elif car_left_right_cntrl == 0:
            motor_fwd_back.run(Raspi_MotorHAT.FORWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.FORWARD)
            motor_left_right.run(Raspi_MotorHAT.RELEASE)
            print("\nCar Forward")
        elif car_left_right_cntrl == 1:
            motor_fwd_back.run(Raspi_MotorHAT.FORWARD)
            motor_fwd_back_2.run(Raspi_MotorHAT.FORWARD)
            motor_left_right.run(Raspi_MotorHAT.FORWARD)
            print("\nCar Forward Right")


def do_cam_control():
    global cam_up_down, cam_left_right, pwm, set_speed
    control_lock = threading.Lock()

    with control_lock:
        cam_up_down_temp = cam_up_down
        cam_left_right_temp = cam_left_right
        set_speed_temp = set_speed

    if cam_up_down_temp == -1 and set_speed_temp > 90:
        pwm.setPWM(14, 0, servo_min)
        print("\nCam Turning down")
        # time.sleep(0.5)
    elif cam_up_down_temp == 1 and set_speed_temp > 90:
        pwm.setPWM(14, 0, servo_max)
        print("\nCam Turning up")
        # time.sleep(0.5)
    elif cam_left_right_temp == -1 and set_speed_temp > 90:
        pwm.setPWM(15, 0, servo_min)
        print("\nCam Turning left")
        # time.sleep(0.5)
    elif cam_left_right_temp == 1 and set_speed_temp > 90:
        pwm.setPWM(15, 0, servo_max)
        print("\nCam Turning right")
        # time.sleep(0.5)
    else:
        pwm.setPWM(14, 0, servo_mid)
        pwm.setPWM(15, 0, servo_mid)
        # print("\nCam Turning neutral")
        # time.sleep(0.5)


# recommended for auto-disabling motors on shutdown!
def turnOffMotors():
    global mh
    mh.getMotor(1).run(Raspi_MotorHAT.RELEASE)
    mh.getMotor(2).run(Raspi_MotorHAT.RELEASE)
    mh.getMotor(3).run(Raspi_MotorHAT.RELEASE)
    mh.getMotor(4).run(Raspi_MotorHAT.RELEASE)
    print("All Motors Turning down")


def reset_all_servos():
    global pwm
    pwm.setPWM(0, 0, servo_mid)
    pwm.setPWM(1, 0, servo_mid)
    pwm.setPWM(14, 0, servo_mid)
    pwm.setPWM(15, 0, servo_mid)
    print("Servo resetted")


# ###########################################################################################
# ################################### MAIN PROGRAM ##########################################
# ###########################################################################################

# create a default object, no changes to I2C address or frequency
mh = Raspi_MotorHAT(addr=0x6F)

# Initialise the PWM device using the default address
pwm = PWM(0x6F)

# Front motor
motor_fwd_back = mh.getMotor(1)
# Back motor
motor_fwd_back_2 = mh.getMotor(2)
# Turn motor Front to left right
motor_left_right = mh.getMotor(3)

# Speed in range of 0 - 255
motor_fwd_back.setSpeed(50)
motor_fwd_back_2.setSpeed(50)
motor_left_right.setSpeed(50)

# Set pwm frequency to 60 Hz
pwm.setPWMFreq(60)

reset_all_servos()
turnOffMotors()

# Calling UDP as thread
thread_rx = threading.Thread(target=get_rx_message)
# Calling Control as thread
thread_control = threading.Thread(target=do_control)

# Making the process run in background
thread_rx.setDaemon(True)
thread_control.setDaemon(True)

# Start thread processing
thread_rx.start()
thread_control.start()

# Wait for threads
thread_rx.join()
thread_control.join()
