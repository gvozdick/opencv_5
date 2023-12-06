import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Moments
import org.opencv.videoio.VideoCapture

import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import java.awt.event.WindowEvent

class imageTest extends JFrame{
    static{System.loadLibrary(Core.NATIVE_LIBRARY_NAME); //грузим opencv
        System.out.println("ver: "+ Core.VERSION)}


    public static void main(String[] args) {
        JFrame window = new JFrame() //создаем окно для отображения видео
        JLabel screen = new JLabel()    //создаем контейнер для изображения
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) //окно закрывается при нажатии на крестик
        window.setVisible(true) //делаем окно видимым

        JFrame window1 = new JFrame() //создаем окно для отображения видео
        JLabel screen1 = new JLabel()    //создаем контейнер для изображения
        window1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) //окно закрывается при нажатии на крестик
        window1.setVisible(true)

        Mat image = Imgcodecs.imread("src/IMG_9678.jpg") // загружаем картинку
        Mat resizeImage = new Mat()
        Size scaleSize = new Size(480,640)
        Imgproc.resize(image, resizeImage,new Size(480,640))

        int a4Length = 297
        int a4Width = 210

        Mat frameGray = new Mat()
        Mat frameBlur = new Mat()
        Mat cannyOutput = new Mat()
        Mat dilatated = new Mat()
        int threshold = 50
        Mat hierarchy = new Mat()
        Scalar color = new Scalar(0,255,0)
        Scalar color1 = new Scalar(0,0,255)
        Scalar color2 = new Scalar(255,0,0)

        MatOfByte buf = new MatOfByte() // байтовая матрица для конвертации изображения для java imageIcon
        MatOfByte buf1 = new MatOfByte()
        ImageIcon ic //объект изображения в формате java
        ImageIcon ic1


//преобразуем в ЧБ и блюрим
        Imgproc.cvtColor(resizeImage, frameGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(frameGray, frameBlur, new Size(5, 5),1.4);
        Mat kernel = new Mat(6,6,CvType.CV_8UC1, new Scalar(1.5))
        Imgproc.dilate(frameBlur,dilatated,kernel)
//ищем контуры
        List contours = new ArrayList()
        List areas = new ArrayList()
        List arcs = new ArrayList()
        Imgproc.Canny(dilatated, cannyOutput, 50, 180)
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        //рисуем контуры
        //Mat drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3)
        if(contours.size()>0) {
            for (int i = 0; i < contours.size(); i++) {
                //Imgproc.drawContours(drawing, contours, i, color, 1,Imgproc.LINE_8,hierarchy,0, new Point());
                Imgproc.drawContours(resizeImage, contours, i, color, 1, Imgproc.LINE_8, hierarchy, 0, new Point());
                areas.add(Imgproc.contourArea(contours[i]))
                MatOfPoint2f m2f = new MatOfPoint2f(contours[i].toArray())
                double arc = Imgproc.arcLength(m2f, true)
                arcs.add(arc)
            }

            //ищем макс площадь контура
            int maxIndex = 0
            double maxArea = 0
            int total = areas.size()
            for (int i = 1; i < areas.size(); i++) {
                if (areas[i] > areas[maxIndex]) {
                    maxIndex = i; maxArea = areas[i]
                }
            }
            println("ind: $maxIndex, area: $maxArea, total: $total")
            println(areas)
            // println(sort(areas))
            println(contours.get(maxIndex).toArray())
            println('--------------')

            //ищем макс длину периметра
            int maxArcIndex = 0
            double maxArc = arcs[maxArcIndex]
            for (int i = 1; i < arcs.size(); i++) {
                if (arcs[i] > arcs[maxArcIndex]) {
                    maxArcIndex = i; maxArc = arcs[i]
                }
            }
            println(arcs)
            println(maxArc)
            println(maxArcIndex)


            //строим Bounding box вокруг максимального контура и находим угловые точки
            MatOfPoint2f m2f = new MatOfPoint2f(contours[maxArcIndex].toArray())
            double arc = Imgproc.arcLength(m2f, true)
            MatOfPoint2f approx = new MatOfPoint2f()
            Imgproc.approxPolyDP(m2f, approx, arc * 0.02, true)
            ArrayList points = new ArrayList()
            for (int i = 0; i < approx.toArray().size(); i++) {
                points.add(approx.toArray()[i])
                Imgproc.circle(resizeImage, points[i], 2, color1, 2)
            }

            //сal center of mass
            Moments moment = Imgproc.moments(approx)
            int x = (moment.get_m10() / moment.get_m00()).toInteger()
            int y = (moment.get_m01() / moment.get_m00()).toInteger()

            Point[] sortedPoints = new Point[4]
            double[] data
            int count = 0
            for (int i = 0; i < approx.rows(); i++) {
                data = approx.get(i, 0)
                double dataX = data[0]
                double dataY = data[1]
                if (dataX < x && dataY < y) {
                    sortedPoints[0] = new Point(dataX, dataY)
                    count++
                } else if (dataX > x && dataY < y) {
                    sortedPoints[1] = new Point(dataX, dataY)
                    count++
                } else if (dataX < x && dataY > y) {
                    sortedPoints[2] = new Point(dataX, dataY)
                    count++
                } else if (dataX > x && dataY > y) {
                    sortedPoints[3] = new Point(dataX, dataY)
                    count++
                }
            }

            //матрицы для warp
            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]
            )
            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(a4Width, 0),
                    new Point(0, a4Length),
                    new Point(a4Width, a4Length)
            )

            //трансформируем
            Mat warpMat = Imgproc.getPerspectiveTransform(src, dst)
            Mat warped = new Mat()
            Imgproc.warpPerspective(resizeImage, warped, warpMat, new Size(a4Width, a4Length))
            //масштабируем
            Imgproc.resize(warped, warped, new Size(a4Width * 2, a4Length * 2))
            Rect rectCrop = new Rect(2, 2, a4Width * 2 - 4, a4Length * 2 - 4)
            Mat cropped = new Mat(warped, rectCrop)

            //ищем контуры на warp изображении
            Imgproc.cvtColor(cropped, frameGray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(frameGray, frameBlur, new Size(5, 5), 1.2);
            Imgproc.dilate(frameBlur, dilatated, kernel)
            List contours1 = new ArrayList()
            List areas1 = new ArrayList()
            List arcs1 = new ArrayList()
            Imgproc.Canny(dilatated, cannyOutput, 50, 120)
            Imgproc.findContours(cannyOutput, contours1, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            //рисуем контуры на warp изображении
            //Mat drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3)
            if (contours1.size() > 0) {
                for (int i = 0; i < contours1.size(); i++) {
                    //Imgproc.drawContours(cropped, contours1, i, color, 1, Imgproc.LINE_8, hierarchy, 0, new Point());
                    areas1.add(Imgproc.contourArea(contours1[i]))
                    MatOfPoint2f m2f1 = new MatOfPoint2f(contours1[i].toArray())
                    double arc1 = Imgproc.arcLength(m2f1, true)
                    arcs1.add(arc1)
                }

                //ищем макс длину периметра
                int maxArcIndex1 = 0
                double maxArc1 = arcs[maxArcIndex1]
                for (int i = 1; i < arcs1.size(); i++) {
                    if (arcs1[i] > arcs1[maxArcIndex1]) {
                        maxArcIndex1 = i; maxArc1 = arcs1[i]
                    }
                }
                println(arcs1)
                println(maxArc1)
                println(maxArcIndex1)

                //строим Bounding box вокруг максимального контура и находим вершины
                MatOfPoint2f m2f1 = new MatOfPoint2f(contours1[maxArcIndex1].toArray())
                RotatedRect rotatedRect = Imgproc.minAreaRect(m2f1)
                Point[] vertices = new Point[4]
                rotatedRect.points(vertices)
                //MatOfPoint points1 = new MatOfPoint(vertices)
                //Imgproc.drawContours(cropped, Arrays.asList(points1), -1, color2, 2)

                //вычисляем размеры объекта
                double pixel = a4Length / (a4Length * 2 - 4)
                println("точки ${vertices.size()} : ${vertices}")
                println("p1: ${vertices[0].x},${vertices[0].y}")
                println("p2: ${vertices[1].x},${vertices[1].y}")
                double objW = ((vertices[0].x - vertices[1].x)**2 + (vertices[0].y - vertices[1].y)**2)**0.5
                objW = objW*pixel
                println("obj width = ${objW}")
                double objL = ((vertices[0].x - vertices[3].x)**2 + (vertices[0].y - vertices[3].y)**2)**0.5
                objL=objL*pixel
                println("obj length = ${objL}")


                //проставляем размеры на кадре
                Scalar color3 = new Scalar(255,0,255)
                Imgproc.arrowedLine(cropped,vertices[0],vertices[1],color3,2)
                Imgproc.putText(cropped,"${objW.round()}",new Point((vertices[0].x+vertices[1].x)/2-40,(vertices[0].y+vertices[1].y)/2),Imgproc.FONT_HERSHEY_SIMPLEX,0.5,color3)
                //Imgproc.arrowedLine(cropped,vertices[1],vertices[0],color3,2)
                Imgproc.arrowedLine(cropped,vertices[0],vertices[3],color3,2)
                Imgproc.putText(cropped,"${objL.round()}",new Point((vertices[0].x+vertices[3].x)/2,(vertices[0].y+vertices[3].y)/2-30),Imgproc.FONT_HERSHEY_SIMPLEX,0.5,color3)
                //Imgproc.arrowedLine(cropped,vertices[3],vertices[0],color3,2)


                //собираем горизонтальный ряд из изображений
                //ArrayList output = [resizeImage]
                //Mat result = new Mat()
                //Core.hconcat(output,result)

                Imgcodecs.imencode(".png", resizeImage, buf) //преобразуем кадр в формат png и записываем в buf
                Imgcodecs.imencode(".png", cropped, buf1)

                ic = new ImageIcon(buf.toArray()) //конвертируем в изображение в формате java imageIcon
                ic1 = new ImageIcon(buf1.toArray())

                screen.setIcon(ic) //помещаем кадр в контейнер
                screen1.setIcon(ic1)

                window.setContentPane(screen) // контейнер привязываем к окну для вывода
                window1.setContentPane(screen1)
                window.pack() //окно по рамеру кадра
                window1.pack()

                //window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)) //закроем окно
            }
        }
        else {
            ic = new ImageIcon(buf.toArray())
            screen.setIcon(ic)
            window.setContentPane(screen)
            window.pack()
        }
        }
}
