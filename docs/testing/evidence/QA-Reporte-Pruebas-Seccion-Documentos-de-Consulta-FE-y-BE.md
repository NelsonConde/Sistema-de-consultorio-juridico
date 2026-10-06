Frontend:
la sección se muestra correctamente dentro del menú para editar la consulta, muestra adecuadamente los archivos correspondientes, la seccion anterior de documentos se encuentra oculta apropiadamente

Endpoint
	/api/consultas/{id}/expediente/archivos?[options]
		Filtro Origen
			  Origen del que si existe un archivo
			Respuesta esperada: 200, lista de archivos existentes
			Respuesta recibida: 200, lista de archivos existentes

			  Origen del que no existen archivos
			Respuesta esperada: 200, lista vacía
			Respuesta recibida: 200, lista vacía

		Filtro Tipo documental
			  Tipo del que si existe un archivo
			Respuesta esperada: 200, lista de archivos existentes
			Respuesta recibida: 200, lista de archivos existentes

			  Tipo del que no existen archivos
			Respuesta esperada: 200, lista vacía
			Respuesta recibida: 200, lista vacía

		Filtro autor
			  Autor del que si existe un archivo
			Respuesta esperada: 200, lista de archivos existentes
			Respuesta recibida: 200, lista de archivos existentes

			  Autor del que no existen archivos
			Respuesta esperada: 200, lista vacía
			Respuesta recibida: 200, lista vacía

		Filtro Tiempo
			  Rango de tiempo válido, existe un archivo
			Respuesta esperada: 200, lista de archivos existentes
			Respuesta recibida: 200, lista de archivos existentes

			  Rango de tiempo válido existen archivos posteriores al rango específicado
			Respuesta esperada: 200, lista vacía
			Respuesta recibida: 200, lista vacía

			  Rango de tiempo válido existen archivos anteriores al rango específicado
			Respuesta esperada: 200, lista vacía
			Respuesta recibida: 200, lista vacía

			  Rango de tiempo inválido
			Respuesta esperada: 400, mensaje de error
			Respuesta recibida: 400, mensaje de error

veredicto: Pasa todas las pruebas

