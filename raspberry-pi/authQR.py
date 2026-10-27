# import the necessary packages
from datetime import datetime
import os
import qrcode
import json
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
from firebase_admin import storage
from uuid import uuid4
from imutils.video import VideoStream
from pyzbar import pyzbar
import argparse
import imutils
import time
import cv2
import serial
import threading

ARD = serial.Serial('/dev/ttyACM1', 9600)

cred = credentials.Certificate(os.environ.get("FIREBASE_KEY_PATH", "serviceAccountKey.json"))  # Firebase 서비스 계정 키 (저장소에 포함하지 않음)
App = firebase_admin.initialize_app(cred, {'databaseURL': os.environ.get("FIREBASE_DB_URL", "https://<project-id>.firebaseio.com/"),
                                           'storageBucket': os.environ.get("FIREBASE_BUCKET", "<project-id>.appspot.com")}, )
ref = db.reference()
bucket = storage.bucket()

placeCode = 'test'

# construct the argument parser and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-o", "--output", type=str, default="barcodes.csv",
                help="path to output CSV file containing barcodes")
args = vars(ap.parse_args())

# From there, let’s initialize our video stream and open our CSV file:
# initialize the video stream and allow the camera sensor to warm up
# 비티오 스트림 초기화 및 카메라 센서가 예열되도록 함
print("[INFO] starting video stream...")

# vs = VideoStream(src=0).start()
# vs = VideoStream(src=0).start()                 # USB 웹캠 카메라 사용시
vs = VideoStream(usePiCamera=True).start()  # 파이 카메라 사용시
time.sleep(2.0)

# open the output CSV file for writing and initialize the set of
# barcodes found thus far
# 작성을 위해 출력된 CSV 파일을 열고, 지금까지 찾은 바코드 세트 초기화
csv = open(args["output"], "w")
found = set()


def ardOpen():
    opencmd = 'x'
    opencmd = opencmd.encode('utf-8')
    ARD.write(opencmd)

def Decode(A):
        A = A.decode()
        A = str(A)
        if A[0] == 'f':
            now = datetime.now()
            date = now.strftime('%Y/%m/%d %H:%M:%S')[2:]
            if A[1] == 'x':
                print('지문인증 : 인증실패 : ' + date)
                newRef = ref.child(placeCode).child('Log').push()
                key = newRef.key

                downloadUrl = '_'

                ref.child(placeCode).child('Log').update({
                    key: {
                        'authCheck': False,
                        'authMethod': '지문인식',
                        'authUrl': downloadUrl,
                        'code': placeCode,
                        'date': date,
                        'name_phoneNo': '-_'
                    }
                })
            elif A[1] == 'o':
                userfingerid = A.split('#')[1]
                print('지문인증 : 인증성공 : ' + date)
                userList = ref.child(placeCode).child('User').get()

                contain = userList.values()
                contain = [*contain]

                for index, list in enumerate(contain):
                    if list['finger'] == int(userfingerid):
                        newRef = ref.child(placeCode).child('Log').push()
                        key = newRef.key

                        downloadUrl = '_'

                        ref.child(placeCode).child('Log').update({
                            key: {
                                'authCheck': True,
                                'authMethod': '지문인식',
                                'authUrl': downloadUrl,
                                'code': placeCode,
                                'date': date,
                                'name_phoneNo': list['name'] + '_' + list['phoneNo']
                            }
                        })
                        break

        return A


def Ardread(): 
    while True:
        # return list [Ard1,Ard2]
        if ARD.readable():
            LINE = ARD.readline()
            code = Decode(LINE)
        else:
            print("읽기 실패 from _Ardread_")

t = threading.Thread(target=Ardread)
t.start()

# Let’s begin capturing + processing frames:
# loop over the frames from the video stream
while True:
    # grab the frame from the threaded video stream and resize it to
    # have a maximum width of 400 pixels
    frame = vs.read()
    frame = imutils.resize(frame, width=400)

    # find the barcodes in the frame and decode each of the barcodes
    # 프레임에서 바코드를 찾고, 각 바코드들 마다 디코드
    barcodes = pyzbar.decode(frame)

    # show the output frame
    cv2.imshow("Barcode Scanner", frame)
    inputKey = cv2.waitKey(1) & 0xFF

    # if the `q` key was pressed, break from the loop
    # q를 누르면 loop를 break함
    if inputKey == ord("q"):
        break


    

    # Let’s proceed to loop over the detected barcodes
    # loop over the detected barcodes
    for barcode in barcodes:

        # extract the bounding box location of the barcode and draw
        # the bounding box surrounding the barcode on the image
        # 이미지에서 바코드의 경계 상자부분을 그리고, 바코드의 경계 상자부분(?)을 추출한다. 
        (x, y, w, h) = barcode.rect
        cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 0, 255), 2)

        # the barcode data is a bytes object so if we want to draw it
        # on our output image we need to convert it to a string first
        # 바코드 데이터는 바이트 객체이므로, 어떤 출력 이미지에 그리려면 가장 먼저 문자열로 변환해야 한다.
        barcodeData = barcode.data.decode("utf-8")
        barcodeType = barcode.type

        # case 1: QR 코드 인증
        # 예시 QR with json
        # barcodeData = '{"code": "test", "name": "홍길동", "phoneNo": "01000000000"}'

        now = datetime.now()
        date = now.strftime('%Y/%m/%d %H:%M:%S')[2:]

        # draw the barcode data and barcode type on the image
        # 이미지에서 바코드 데이터와 테입(유형)을 그린다
        text = "{} ({})".format(barcodeData, barcodeType)
        cv2.putText(frame, text, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        # 오류(예외) 처리
        # 1. 해당qr이아닌경우 - json이아닌경우, json: 해당 key 가없는 qr일경우
        # 2. 사용자 정보가 없는 qr일 경우, 사용자 정보가 다른 qr일 경우
        def authFailedLog():
            print('QR인증 : 인증실패 : ' + date)

            newRef = ref.child(placeCode).child('Log').push()
            key = newRef.key

            blob = bucket.blob(placeCode + '/Log/' + key + '/QRAuth')
            new_token = uuid4()
            metadata = {"firebaseStorageDownloadTokens": new_token}
            blob.metadata = metadata

            img = qrcode.make(barcodeData)
            img.save('./qr/auth.png')

            blob.upload_from_filename('./qr/auth.png')
            downloadUrl = 'https://firebasestorage.googleapis.com/v0/b/' + bucket.name + '/o/' + placeCode \
                          + '%2F' + 'Log' + '%2F' + key + '%2FQRAuth?alt=media&token=' + str(new_token)
            os.remove("./qr/auth.png")

            ref.child(placeCode).child('Log').update({
                key: {
                    'authCheck': False,
                    'authMethod': 'QR코드',
                    'authUrl': downloadUrl,
                    'code': placeCode,
                    'date': date,
                    'name_phoneNo': '-_'
                }
            })


        if barcodeData[0] != '{' or barcodeData[-1] != '}':
            authFailedLog()
        else:

            json_data = json.loads(barcodeData)

            userList = ref.child(placeCode).child('User').get()
            userList = str(userList)

            if 'code' not in json_data:
                authFailedLog()
            else:
                if json_data['code'] == placeCode:
                    if 'name' not in json_data or 'phoneNo' not in json_data:
                        authFailedLog()
                    else:
                        qrName_phoneNo = json_data['name'] + '_' + json_data['phoneNo']
                        if ("'name_phoneNo': '" + qrName_phoneNo + "'") in userList:

                            print('QR인증 : 인증성공 : ' + qrName_phoneNo + ' : ' + date)

                            # arduino (serial) open door cmd
                            ardOpen()
                            # firebase db/storage put Log data
                            newRef = ref.child(placeCode).child('Log').push()
                            key = newRef.key

                            blob = bucket.blob(placeCode + '/Log/' + key + '/QRAuth')
                            new_token = uuid4()
                            metadata = {"firebaseStorageDownloadTokens": new_token}
                            blob.metadata = metadata

                            img = qrcode.make(barcodeData)
                            img.save('./qr/auth.png')

                            blob.upload_from_filename('./qr/auth.png')
                            downloadUrl = 'https://firebasestorage.googleapis.com/v0/b/' + bucket.name + '/o/' \
                                          + placeCode + '%2F' + 'Log' + '%2F' + key + '%2FQRAuth?alt=media&token=' \
                                          + str(new_token)
                            os.remove("./qr/auth.png")

                            ref.child(placeCode).child('Log').update({
                                key: {
                                    'authCheck': True,
                                    'authMethod': 'QR코드',
                                    'authUrl': downloadUrl,
                                    'code': placeCode,
                                    'date': date,
                                    'name_phoneNo': qrName_phoneNo
                                }
                            })
                        else:
                            authFailedLog()

                else:
                    authFailedLog()

        # if the barcode text is currently not in our CSV file, write
        # the timestamp + barcode to disk and update the set
        # 현재 바코드 텍스트가 CSV 파일안에 없을경우, timestamp, barcode를 작성하고 업데이트
        if barcodeData not in found:
            csv.write("{},{}\n".format(datetime.now(),
                                       barcodeData))
            csv.flush()
            found.add(barcodeData)

        time.sleep(1)

# close the output CSV file do a bit of cleanup
print("[INFO] cleaning up...")
csv.close()
cv2.destroyAllWindows()
vs.stop()
