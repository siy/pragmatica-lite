package org.pragmatica.http.example.qrgenerator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface QrGeneratorService {
    QRCodeWriter WRITER = new QRCodeWriter();
    String FORMAT = "PNG";

    static Result<byte[]> generateQR(String text, int width, int height) {
        var pngOutputStream = new ByteArrayOutputStream();

        return Result.lift(QrGeneratorService::exceptionMapper, () -> {
            MatrixToImageWriter.writeToStream(
                WRITER.encode(text, BarcodeFormat.QR_CODE, width, height),
                FORMAT,
                pngOutputStream);

            return pngOutputStream.toByteArray();
        });
    }

    static Cause exceptionMapper(Throwable throwable) {
        return switch (throwable) {
            case IllegalArgumentException e -> new QrEncoderError.IllegalArgument(e.getMessage());
            case WriterException e -> new QrEncoderError.EncodingError(e.getMessage());
            case IOException e -> new QrEncoderError.WritingError(e.getMessage());
            default -> new QrEncoderError.UnexpectedError(throwable.getMessage());
        };
    }
}
