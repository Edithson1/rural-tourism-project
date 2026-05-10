# 1. Introducción

## Objetivo del proyecto

**Yupay Turismo** es una aplicación móvil offline‑first diseñada para comunidades rurales del Perú, que transforma la recolección manual de datos turísticos en información útil, accesible y accionable. Nace como una propuesta de solución al desafío público *“Del cuaderno al insight”* planteado por el Ministerio de Comercio Exterior y Turismo (MINCETUR) y el Programa ProInnovate, cuyo propósito es empoderar a los emprendedores turísticos y mejorar la toma de decisiones públicas a partir de datos confiables.

El proyecto tiene como finalidad desarrollar un prototipo funcional abordando los siguientes objetivos:

1. Registrar visitantes de manera sencilla y offline en entornos con conectividad limitada.
2. Transformar esos datos en insights visuales y multimodales (texto simplificado, gráficos y audio en quechua y español).
3. Probar el prototipo mediante pruebas internas, utilizando escenarios simulados que reflejan las condiciones típicas de comunidades quechuahablantes como **Luquina (Puno) y Misminay (Cusco)**. De esta forma se garantiza que la solución sea pertinente para el contexto andino y que su arquitectura permita, en una fase posterior, incorporar otras lenguas originarias y extenderse a otras comunidades.

## Breve descripción de la aplicación

**Yupay Turismo** reemplaza el tradicional cuaderno de registro por una interfaz móvil adaptada a dispositivos de baja gama. La app permite al emprendedor registrar cada visita turística (procedencia, gasto aproximado, servicios consumidos) sin necesidad de conexión a internet. Los datos se almacenan localmente y se sincronizan de forma inteligente cuando el dispositivo encuentra una red Wi‑Fi o datos móviles.

Posteriormente, la aplicación procesa la información y genera:
- Paneles de control con gráficos simples y al menos dos elementos pictográficos (mapa de procedencias, termómetro de visitas).
- Recomendaciones personalizadas según el tipo de emprendimiento (hospedaje, alimentación o artesanía).
- Audios en quechua y español que leen los insights en voz alta, utilizando modelos de síntesis de voz de código abierto (Meta MMS + Sherpa‑ONNX), facilitando la comprensión en contextos de baja alfabetización o predominio de lenguas originarias.

La solución está alineada con los lineamientos de interoperabilidad del MINCETUR y cumple con principios de privacidad y protección de datos personales. Su diseño modular permitirá, como trabajo futuro, escalar el soporte multilingüe y adaptar los formularios a nuevas tipologías de emprendimiento.