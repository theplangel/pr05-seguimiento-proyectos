FROM python:3.12-alpine
WORKDIR /app
COPY simulador.py .
RUN adduser -D -u 10001 sim
USER sim
ENV PYTHONUNBUFFERED=1
CMD ["python", "simulador.py"]
