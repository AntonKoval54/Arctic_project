def modify_temperatures(filename, season, new_temperature):
  """Изменяет температуру воздуха и грунта в указанном сезоне.

  Args:
    filename (str): Имя файла с данными.
    season (str): Сезон для изменения (зима, лето).
    new_temperature (float): Новая температура для изменения.
  """

  with open(filename, 'r') as file:
    lines = file.readlines()

  # Определяем дни зимы и лета
  winter_days = list(range(1, 90+1))+list(range(366, 458))
  spring_days = list(range(91, 185+1))+list(range(458, 548))
  summer_days = list(range(186, 277+1))+list(range(548, 638))
  autumn_days = list(range(278, 366))+list(range(638, 726))

  # Изменяем температуру в указанном сезоне
  if season == "зима":
    days = winter_days
  elif season == "лето":
    days = summer_days
  elif season == "весна":
    days = spring_days
  elif season == "осень":
    days = autumn_days
  else:
    print("Неверный сезон. Допустимые значения: зима, лето, весна, осень")
    return

  for i, line in enumerate(lines):
    if i in days:
      parts = [float(i) for i in line.split()]
      lines[i] = f"{round(parts[0]+new_temperature,5)} {round(parts[1]+new_temperature, 5)}\n"

  # Записываем измененные данные в файл
  with open(filename, 'w') as file:
    file.writelines(lines)

# Пример использования:
modify_temperatures(r"D:\Загрузки\ПП_5сем\arcticUrFU2024\arcticUrFU2024\program_uran\days_change_data.txt", "зима", -10) # Изменяем температуру зимой на -10 градусов
#modify_temperatures("days.txt", "лето", 50) # Изменяем температуру летом на 25 градусов
