# Artic_project

проект 5 семестр исследование арктики 

Были проведены исследования скважин согласно задания 

!!! ЗАДАНИЕ I. 

1. Провести расчет 1 года (365 дней).
2. Для своей термометрической скважины построить профили температур для всех 12ти месяцев. 
   Данные - в point<N>Zstep.dat - для каждой котнтрольной точки расположение узлов по z и значение температуры в этом узле [C].
   Вариант реализации можно выбрать самостоятельно: от обработки в Excel до написания собственной программы. 
3. Посмотреть и описать динамику изменения верхнего оттаивающего слоя (ALT) в течение года. 
4. Изменить годичный цикл температуры воздуха и поверхности грунта (файл days.txt), просчитать год с новыми данными, 
   посмотреть как изменяется температура в грунте и глубина протаивания.
5. Сделать выводы.

!!! ЗАДАНИЕ II.

1. Найти на сайте https://monitoring.arctic.yanao.ru/ 
   здание г.Салехард, ул. Зои Космодемьянской, д. 68, термометрическую скважину со своим номером и сравнить профили температуры в грунте с рассчитанными. 
2. Изменить модель режима работы СОУ (подпрограмма zad_SOU()) и найти вариант режима работы, дающий более точное совпадение рассчитанных и реальных данных. 
3. Обосновать выбор оптимальной модели.   

!!! ЗАДАНИЕ III*.

Исследовать влияние тренда потепления (temptrend.txt) на температуру в грунте на 50 лет вперед с работающими и не работающими СОУ. 
Возможный диапазон тренда потепления: m0.02 - 0.08 [C/год].

![image](https://github.com/user-attachments/assets/b1412559-ffec-4b74-9b7f-2d767b20bd3d)

Создан парсер выходных данных с супер компьютера

Начали делать интерфейс это оптимизирует работу так как сейчас надо использовать putty / winscp / excel

Подбираем коэффиценты работы СОУ для более точного моделирования
