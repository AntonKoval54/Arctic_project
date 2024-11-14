import paramiko
import time

class SSHClient:
    def __init__(self, hostname, username, key_filename, passphrase):
        self.hostname = hostname
        self.username = username
        self.key_filename = key_filename
        self.passphrase = passphrase
        self.client = paramiko.SSHClient()
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(hostname=self.hostname, username=self.username, key_filename=self.key_filename, passphrase=self.passphrase)
        self.channel = self.client.get_transport().open_session()
        self.channel.get_pty()
        self.channel.invoke_shell()

    def send_command(self, command):
        self.channel.send(command + "\n")

        while True:
            if self.channel.recv_ready():
                output = self.channel.recv(1024).decode("utf-8")
                if output.startswith(command):
                    output = output[len(command):]
                print(output)
            else:
                time.sleep(0.5)
                if not (self.channel.recv_ready()):
                    break

    def close(self):
        self.client.close()

if __name__ == "__main__":
    # Параметры подключения
    hostname = 'umt.imm.uran.ru'
    username = 's0116'
    key_filename = r'D:\Загрузки\private_key_2_openssh.ppk'
    passphrase = 'pp5sem'

      # Создание объекта класса
    ssh_client = SSHClient(hostname, username, key_filename, passphrase)

    # Вывод начального сообщения сервера
    if ssh_client.channel.recv_ready():
        output = ssh_client.channel.recv(1024).decode("utf-8")
        print(output)

    # Основной цикл взаимодействия
    while True:
        command = input('$ ')
        if command == 'exit':
            break

        ssh_client.send_command(command)

    # Закрытие соединения
    ssh_client.close()