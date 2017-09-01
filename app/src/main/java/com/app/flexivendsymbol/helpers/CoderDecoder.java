package com.app.flexivendsymbol.helpers;

public class CoderDecoder {

    public long Decode(long code) {
        long id = code / 24;
        if (Encode(id) == code)
            return id;

        return -1;
    }

    public long Encode(long id) {
        long code = id;
        //calc check sum
        {
            long sum = GetSumOfDigits(id);
            code = id * 24 + (sum + 3) % 24;
        }
        return code;
    }

    long GetSumOfDigits(long code) {
        long sum = 0L;
        for (int i = 0; i < 11; i++) {
            sum += code % 24;
            code /= 24;
        }
        return sum;
    }

    public String EncodeToString(long number, int minLength) {
        //encode
        long encoded = Encode(number);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minLength; i++) {
            long n = encoded % 24;
            sb.insert(0, DigitToChar(n));
            encoded /= 24;
        }

        return "O" + sb.toString();
    }

    public static char DigitToChar(long digit) {
        if (digit <= 9)
            return (char) ('0' + digit);
        else
            return (char) ('A' + (digit - 10));
    }

    public static int CharToDigit(char c) {
        if (Character.isDigit(c))
            return c - '0';
        else
            return 10 + (c - 'A');
    }
}