import paramiko
import time

if __name__ == "__main__":
    # Параметры подключения
    hostname = 'umt.imm.uran.ru'
    username = 's0116'
    key_filename = r'D:\Загрузки\private_key_2_openssh.ppk'
    passphrase = 'pp5sem'

    # Создание объекта класса
    ssh_client = paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh_client.connect(hostname=hostname, username=username, key_filename=key_filename, passphrase=passphrase)

    channel = ssh_client.get_transport().open_session()
    channel.get_pty()
    channel.invoke_shell()

    if channel.recv_ready():
        output = channel.recv(1024).decode("utf-8")
        print(output)

    while True:
        command = input('$ ')
        if command == 'exit':
            break

        channel.send(command + "\n")

        while True:
            if channel.recv_ready():
                output = channel.recv(1024).decode("utf-8")
                if output.startswith(command):
                    output = output[len(command):]
                print(output)
            else:
                time.sleep(0.5)
                if not (channel.recv_ready()):
                    break

    ssh_client.close()