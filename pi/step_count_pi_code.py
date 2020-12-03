import aioblescan as aiobs
from aioblescan.plugins import EddyStone
import asyncio
from datetime import datetime
import paho.mqtt.client as mqtt
import time
import datetime
import os
import random


broker_address = "192.168.4.1"
publishtopic1 = "steps"
publishtopic2 = "calories"
subscribetopic1 = "weather"
subscribetopic2 = "personal"

def _process_packet(data):
    ev = aiobs.HCI_Event()
    xx = ev.decode(data)
    xx = EddyStone().decode(ev)
    if xx:
        pwd = xx.get('url')[:13]
        if pwd == 'http://iot2/?':
            steps = xx.get('url')[13:]
            now = datetime.datetime.now().strftime('%A, %B %d at %I:%M %p')
            print('The step count received at {0} is {1}'.format(now, steps))
            set_steps(steps)

def on_message(client, userdata, message):
    if message.topic == 'weather':
        forecast = eval(str(message.payload.decode("utf-8")))

        goal = predict_steps(*forecast['today'], today=True)

        steps = get_steps()
        if steps >= goal:
            met_goal = 'You met your goal!'
        else:
            met_goal = 'Keep going!'

        update_weights(steps, goal, forecast['today'])

        client.publish(publishtopic1, met_goal)
        goal_tom = predict_steps(*forecast['tomorrow'], today=False)
        if goal_tom <= 0:
            goal_tom = 0
        print('You have walked {0} out of {1} steps today'.format(steps,int(goal)))
        print('Your step goal for tomorrow is {0}'.format(int(goal_tom)))

    if message.topic == 'personal':
        steps = get_steps()
        try:
            personal = eval(str(message.payload.decode('utf-8')))
            height, weight, age, goal = personal
            cals_burned = estimate_calories(steps, height, weight, age)
        except Exception as e:
            print(e)
        if cals_burned >= goal:
            met_cal_goal = True
        else:
            met_cal_goal = False
        client.publish(publishtopic2, met_cal_goal)
        print('You have burned {0} of {1} calories today'.format(cals_burned, goal))

def estimate_calories(steps, height, weight, age):
    calorie_factor = 0.04
    bmi_factor = 703 * (weight / (height**2))
    age_factor = age / 30
    bmi_factor = bmi_factor / 20
    return steps * calorie_factor * bmi_factor * age_factor

def estimate_calories_simple(steps):
    calorie_factor = 0.04
    return steps * calorie_factor

def predict_steps(high, low, hum, today):
    weights = get_weights(today=today)
    w0,w1,w2,w3 = weights
    steps_predicted = w0 + w1*high + w2*low + w3*hum
    return steps_predicted

def update_weights(step_actual, step_pred, weather):
    weights = get_weights(today=True)
    err = step_pred - step_actual
    weather = (1, *weather)
    pairs = list(zip(weights, weather))
    theta = 0.0001
    updated_weights = list(map(lambda x: x[0] - (theta * err * x[1]), pairs))
    set_weights(updated_weights)

def set_weights(weights, today=False):
    if os.path.isfile('./weights.txt') and os.path.getsize('./weights.txt') > 0:
        with open('./weights.txt', 'r') as f:
            d = eval(f.read())
    else:
        d = dict()
    if today:
        dt = datetime.datetime.now().strftime('%m/%d/%Y')
    else:
        dt = datetime.datetime.now() + datetime.timedelta(days=1)
        dt = dt.strftime('%m/%d/%Y')
    d[dt] = weights
    with open ('./weights.txt', 'w') as f:
        print(d, file=f)

def get_weights(today):
    try:
        with open('./weights.txt', 'r') as f:
            if today:
                weights = eval(f.read())[datetime.datetime.now().strftime('%m/%d/%Y')]
            else:
                dt = datetime.datetime.now() + datetime.timedelta(days=1)
                dt = dt.strftime('%m/%d/%Y')
                weights = eval(f.read())[dt]
    except Exception as e:
        weights = [3697.766801, 64.69992133, 283.57456519, -109.19405416]
        set_weights(weights, today=True)
    return weights

def set_steps(steps):
    if os.path.isfile('./step_log.txt') and os.path.getsize('./step_log.txt') > 0:
        with open('./step_log.txt', 'r') as f:
            step_log = eval(f.read())
    else:
        step_log = dict()
    with open('./step_log.txt', 'w') as f:
        step_log[datetime.datetime.now().strftime('%m/%d/%Y')] = steps
        print(step_log, file=f)

def get_steps():
    step_log = None
    if os.path.isfile('./step_log.txt') and os.path.getsize('./step_log.txt') > 0:
        with open('./step_log.txt', 'r') as f:
            step_log = eval(f.read())
    if not step_log:
        steps = 0
    else:
        if datetime.datetime.now().strftime('%m/%d/%Y') in step_log.keys():
            steps = step_log[datetime.datetime.now().strftime('%m/%d/%Y')]
        else:
            steps = 0
        return int(steps)

def generate_fake_data():
    weights = [3697.766801, 64.69992133, 283.57456519, -109.19405416]
    orig = weights
    for i in range(30):
        # Generate the data
        high = random.randint(60, 90)
        low = random.randint(40, 59)
        hum = random.randint(0, 100)
        weather = (high, low, hum)
        if high >= 60 and high <= 69:
            step_actual = 1000
        if high >= 70 and high <= 79:
            step_actual = 10000
        if high >= 80 and high <= 90:
            step_actual = 5000
        step_pred = predict_fake_steps(high, low, hum, weights)
        orig_pred = predict_fake_steps(high, low, hum, orig)
        if step_pred <= 0:
            step_pred = 0
        weights = update_fake_weights(step_actual, step_pred, weather, weights) 
        print('Day {0}'.format(i+1))
        print('Pred: {0}'.format(int(step_pred)))
        print('Orig: {0}'.format(int(orig_pred)))
        print()

def update_fake_weights(step_actual, step_pred, weather, weights):
    err = step_pred - step_actual
    weather = (1, *weather)
    pairs = list(zip(weights, weather))
    theta = 0.0001
    updated_weights = list(map(lambda x: x[0] - (theta * err * x[1]), pairs))
    return updated_weights

def predict_fake_steps(high, low, hum, weights):
    w0,w1,w2,w3 = weights
    steps_predicted = w0 + w1*high + w2*low + w3*hum
    return steps_predicted

#generate_fake_data()

if __name__ == '__main__':
    client = mqtt.Client('P1')
    client.on_message = on_message
    client.connect(broker_address)
    client.loop_start()
    client.subscribe(subscribetopic1)
    client.subscribe(subscribetopic2)
    mydev = 0
    event_loop = asyncio.get_event_loop()
    mysocket = aiobs.create_bt_socket(mydev)
    fac = event_loop._create_connection_transport(mysocket,aiobs.BLEScanRequester,None,None)
    conn, btctrl = event_loop.run_until_complete(fac)
    btctrl.process = _process_packet
    btctrl.send_scan_request()
    try:
        event_loop.run_forever()
    except KeyboardInterrupt:
        print('keyboard interrupt')
    finally:
        print('closing event loop')
        btctrl.stop_scan_request()
        conn.close()
        event_loop.close()
        client.loop_stop()
