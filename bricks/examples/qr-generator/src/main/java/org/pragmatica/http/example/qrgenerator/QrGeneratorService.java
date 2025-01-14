package org.pragmatica.http.example.qrgenerator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface QrGeneratorService {
    Logger log = LoggerFactory.getLogger(QrGeneratorService.class);
    QRCodeWriter WRITER = new QRCodeWriter();
    String FORMAT = "PNG";

    static Result<byte[]> generateQR(String text, int width, int height) {
        if (text == null) {
            return new QrEncoderError.IllegalArgument("URL to encode is required").result();
        }

        var pngOutputStream = new ByteArrayOutputStream();

        return Result.lift(QrGeneratorService::exceptionMapper, () -> {
                         MatrixToImageWriter.writeToStream(
                             WRITER.encode(text, BarcodeFormat.QR_CODE, width, height),
                             FORMAT,
                             pngOutputStream);

                         return pngOutputStream.toByteArray();
                     })
                     .onSuccessRun(() -> log.debug("QR code generated for {}", text))
                     .onFailure(error -> log.debug("QR code generation failed for {} {}", text, error));
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
