import sys
from PySide6.QtWidgets import (
    QApplication,
    QWidget,
    QVBoxLayout,
    QHBoxLayout,
    QLineEdit,
    QTextEdit,
    QPushButton,
)
from PySide6.QtCore import QThread, Signal

from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure
import matplotlib.pyplot as plt
import pandas as pd
import pyqtgraph as pg

import paramiko
import time


class SSHClient(QThread):
    output_signal = Signal(str)

    def __init__(self, hostname, username, key_filename, passphrase):
        super().__init__()
        self.hostname = hostname
        self.username = username
        self.key_filename = key_filename
        self.passphrase = passphrase
        self.client = None
        self.channel = None

    def run(self):
        self.client = paramiko.SSHClient()
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(
            hostname=self.hostname,
            username=self.username,
            key_filename=self.key_filename,
            passphrase=self.passphrase,
        )
        self.channel = self.client.get_transport().open_session()
        self.channel.get_pty()
        self.channel.invoke_shell()

        # Вывод начального сообщения сервера
        if self.channel.recv_ready():
            output = self.channel.recv(1024).decode("utf-8")
            self.output_signal.emit(output)

        while True:
            if self.channel.recv_ready():
                output = self.channel.recv(1024).decode("utf-8")
                self.output_signal.emit(output)

    def send_command(self, command):
        self.channel.send(command + "\n")

    def close(self):
        if self.client:
            self.client.close()


class MainWindow(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("SSH Terminal")

        # Параметры подключения
        self.hostname = 'umt.imm.uran.ru'
        self.username = 's0116'
        self.key_filename = r'D:\Загрузки\private_key_2_openssh.ppk'
        self.passphrase = 'pp5sem'

        # Создаем SSH-клиент
        self.ssh_client = SSHClient(
            self.hostname, self.username, self.key_filename, self.passphrase
        )

        # Создаем элементы интерфейса
        self.input_line = QLineEdit()
        self.output_area = QTextEdit()
        #self.output_area.setReadOnly(True)
        self.send_button = QPushButton("Send")
        self.make_button = QPushButton("make")

        # Создаем макеты
        main_layout = QHBoxLayout()
        left_layout = QVBoxLayout()
        #middle_layout = QVBoxLayout()
        right_layot = QVBoxLayout()

        canvas = self.plot_maker()

        # Размещаем элементы
        left_layout.addWidget(self.output_area)
        left_layout.addWidget(self.input_line)
        left_layout.addWidget(self.send_button)
        right_layot.addWidget(canvas)
        right_layot.addWidget(self.make_button)

        main_layout.addLayout(left_layout)
        main_layout.addLayout(right_layot)

        # Устанавливаем макет для главного окна
        self.setLayout(main_layout)

        # Подключаем сигналы и слоты
        self.send_button.clicked.connect(self.send_command)
        self.ssh_client.output_signal.connect(self.update_output)

        self.make_button.clicked.connect(self.plot_maker)

        # Запускаем SSH-клиент в отдельном потоке
        self.ssh_client.start()

    def send_command(self):
        command = self.input_line.text()
        self.input_line.clear()
        self.ssh_client.send_command(command)

    def update_output(self, text):
        self.output_area.append(text)

    def plot_maker(self):
        # Загружаем табличные данные
        data = {
            'Дата': ['2021-12-01', '2022-01-01', '2022-02-01', '2022-03-01'],
            '0': [-14.976375, -19.735125, -18.487775, 0],
            '0.5': [-8.43015, -15.0273, -14.0145, -7.153553],
            '1': [-1.61934, -9.77852, -10.97714, -8.876696],
            '1.5': [0.105824, -5.7805, -9.10713, -9.0902],
            '2': [0.131504, -2.57181, -7.89908, -8.81523],
            '2.5': [-0.30222, -1.07753, -7.09578, -8.388668],
            '3': [-0.79077, -1.86419, -6.65643, -7.9347],
            '3.5': [-1.42483, -2.65959, -6.25869, -7.54084],
        }
        df = pd.DataFrame(data)

        # Создаем фигуру и canvas для matplotlib
        figure = Figure()
        canvas = FigureCanvas(figure)

        # Создаем axes для графика
        axes = figure.add_subplot(111)

        # Строим график
        axes.plot(df['Дата'], df['0.5'],  label='0.5')
        axes.plot(df['Дата'], df['1.5'],  label='1.5')
        axes.plot(df['Дата'], df['2.5'],  label='2.5')

        # Устанавливаем подписи осей
        axes.set_xlabel('Значение')
        axes.set_ylabel('Дата')

        # Добавляем легенду
        axes.legend()

        return canvas


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = MainWindow()
    window.show()
    sys.exit(app.exec())
