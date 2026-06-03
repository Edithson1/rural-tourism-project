package upch.mluque.final_project.utils

object UiTranslations {
    private val translations = mapOf(
        "Español" to mapOf(
            // Onboarding
            "onboarding_title_0" to "Registra visitas sin internet",
            "onboarding_desc_0" to "Lleva el control de tus visitantes en cualquier lugar, incluso sin conexión a datos o Wi-Fi.",
            "onboarding_btn_0" to "Comenzar",
            "onboarding_title_1" to "Mira tus resultados",
            "onboarding_desc_1" to "Entiende cómo le va a tu emprendimiento con gráficos sencillos y fáciles de leer.",
            "onboarding_btn_1" to "Siguiente",
            "onboarding_title_2" to "Recibe consejos",
            "onboarding_desc_2" to "Escucha recomendaciones personalizadas en voz alta en quechua o español para mejorar tus servicios.",
            "onboarding_btn_2" to "Crear mi perfil",
            "onboarding_link_device" to "vincular como dispositivo adicional",
            "onboarding_perm_title" to "Permisos de Red",
            "onboarding_perm_desc" to "Para vincular este dispositivo, necesitamos permisos para gestionar la conexión WiFi.",
            
            // Home
            "home_greeting" to "¡Hola, %s!",
            "home_category" to "Rubro: %s",
            "home_tip_title" to "Tip de Emprendedor",
            "home_recent_visits" to "Registros Recentes",
            "home_tourist_country" to "Turista / País",
            "home_est_expense" to "Gasto Est.",
            "home_no_records" to "No hay registros disponibles",
            "time_day" to "Día",
            "time_week" to "Sem",
            "time_month" to "Mes",
            "time_year" to "Año",
            "chart_today" to "Visitas de hoy",
            "chart_week" to "Visitas de la semana",
            "chart_month" to "Visitas por semana (Mes)",
            "chart_year" to "Visitas por mes (Año)",

            // Profile Setup
            "setup_title" to "Configura tu perfil",
            "setup_business_name_label" to "Nombre de la tienda o emprendimiento",
            "setup_business_name_hint" to "Ej. Artesanías del Valle",
            "setup_service_question" to "¿Qué servicio ofreces principalmente?",
            "setup_save_btn" to "GUARDAR Y ENTRAR",

            // Map
            "map_title" to "Mapa de Visitas",
            "map_legend" to "Leyenda de Servicios",
            "map_summary_title" to "Resumen de Mapa",
            "map_listen_summary" to "Escuchar Resumen",
            "map_expand" to "Expandir",

            // Tip Detail
            "tip_detail_title" to "Tip de Emprendedor",
            "tip_detail_subtitle" to "Tu consejo del día",

            // Profile
            "profile_title" to "Mi Perfil",
            "profile_edit" to "Editar",
            "profile_settings_title" to "Configuración de la App",
            "profile_language" to "Idioma",
            "profile_voice_speed" to "Velocidade da voz",
            "profile_link_device" to "Vincular nuevo dispositivo",
            "profile_linked_devices" to "Dispositivos vinculados",
            "profile_logout" to "Cerrar sesión",
            "profile_info_title" to "Información",
            "profile_help" to "Ayuda y Soporte",
            "profile_privacy" to "Política de Privacidad",
            "profile_edit_title" to "Editar Perfil",
            "profile_photo_change" to "Cambiar foto de perfil",
            "profile_business_name" to "Nombre del emprendimiento",
            "profile_sector" to "Sector o Rubro",
            "profile_save_changes" to "GUARDAR CAMBIOS",
            "profile_logout_confirm_title" to "¿Cerrar sesión?",
            "profile_logout_confirm_desc" to "Se cerrará la sesión actual y el dispositivo volverá a la pantalla inicial. Deberás volver a vincularte para sincronizar datos.",
            "profile_online" to "En línea",
            "profile_offline" to "Desconectado",

            // Services
            "service_lodging" to "Hospedaje",
            "service_food" to "Alimentación",
            "service_handicraft" to "Artesanía",
            "service_others" to "Varios",

            // Visits
            "visits_title" to "Registros de Visitas",
            "visits_add" to "Añadir Visita",
            "visits_search_hint" to "Buscar por país...",
            "visits_detail_title" to "Detalle de la Visita",
            "visits_date" to "Fecha",
            "visits_country" to "País",
            "visits_price" to "Gasto Estimado",
            "visits_services" to "Servicios Prestados",
            "add_visit_title" to "Registrar Nueva Visita",
            "add_visit_country_label" to "País de origen",
            "add_visit_price_label" to "Gasto estimado",
            "add_visit_services_label" to "Servicios prestados",
            "add_visit_save" to "GUARDAR REGISTRO",
            "select_option" to "Seleccionar opción",
            "select_range" to "Seleccionar rango",
            "record_label" to "Registro",
            "sent_record" to "Registro Enviado",
            "pending_record" to "Pendiente de Envío",
            "sent_on" to "Enviado el",

            // Time Ago & Categories
            "time_now" to "Ahora mismo",
            "time_min_ago" to "Hace %d min",
            "time_hours_ago" to "Hace %d horas",
            "time_hour_ago" to "Hace 1 hora",
            "time_yesterday" to "Ayer",
            "time_days_ago" to "Hace %d días",
            "cat_today" to "Hoy",
            "cat_yesterday" to "Ayer",
            "cat_this_week" to "Esta semana",
            "cat_this_month" to "Este mes",
            "cat_this_year" to "Este año",
            "cat_older" to "Más de un año",
            "total_records" to "%d total",
            "no_records_yet" to "No hay registros aún",
            "tourist_label" to "Turista",

            // Info Screens
            "help_content" to "Aquí puedes encontrar información sobre cómo usar la aplicación Yupay Turismo. \n\n1. Registro de visitas: Ve a la pestaña de Visitas o Home y presiona el botón +.\n2. Mapa: Visualiza el origen de tus visitantes en tiempo real.\n3. Perfil: Configura tus datos y preferencias de la aplicación.",
            "privacy_content" to "Tu privacidad es importante para nosotros. Los datos recolectados en esta aplicación se guardan de forma local en tu dispositivo.\n\nNo compartimos información personal con terceros sin tu consentimiento explícito. Los datos de visitas son utilizados únicamente para generar tus reportes estadísticos.",
            
            // General
            "btn_cancel" to "CANCELAR",
            "btn_confirm" to "CONFIRMAR",
            "btn_close" to "CERRAR",
            "btn_back" to "Atrás"
        ),
        "Quechua" to mapOf(
            // Onboarding
            "onboarding_title_0" to "Mana internetniyuq watukuqkunata qillqay",
            "onboarding_desc_0" to "Maypipas watukuqniykikunata kamachiy, mana internetniyuq kaspapas.",
            "onboarding_btn_0" to "Qallariy",
            "onboarding_title_1" to "Llamkayniykipa rurunta qhaway",
            "onboarding_desc_1" to "Llamkayniyki imayna kachkasqanta yachay sasan qhawana siq'ikunawan.",
            "onboarding_btn_1" to "Qatiqnin",
            "onboarding_title_2" to "Yuyaychaykunata chaskiy",
            "onboarding_desc_2" to "Llamkayniyki aswan allin kananpaq, quechuapi otaq españolpi rimaykunata uyariy.",
            "onboarding_btn_2" to "Ñuqaq qillqayniy ruray",
            "onboarding_link_device" to "huk q'illayta t'inkiy",
            "onboarding_perm_title" to "Llika unanchakuna",
            "onboarding_perm_desc" to "Kay q'illayta t'inkinapaq, WiFi llika kamachinapaq unanchakuna necesitayku.",
            
            // Home
            "home_greeting" to "¡Napaykullayki, %s!",
            "home_category" to "Llank'ay: %s",
            "home_tip_title" to "Yachayniyuqpa yuyaychaynin",
            "home_recent_visits" to "Qhipa qillqasqakuna",
            "home_tourist_country" to "Purikuq / Suyu",
            "home_est_expense" to "Chaninchasqa qullqi",
            "home_no_records" to "Manan kanchu qillqasqakuna",
            "time_day" to "P'unchay",
            "time_week" to "Sem",
            "time_month" to "Killa",
            "time_year" to "Wata",
            "chart_today" to "Kunan p'unchay watukuykuna",
            "chart_week" to "Semana watukuykuna",
            "chart_month" to "Semana watukuykuna (Killa)",
            "chart_year" to "Killa watukuykuna (Wata)",

            // Profile Setup
            "setup_title" to "Qillqayniykita allichay",
            "setup_business_name_label" to "Llank'ayniykipa sutin",
            "setup_business_name_hint" to "Kayna: Suyu makipi rurasqakuna",
            "setup_service_question" to "¿Ima yanapakuyta aswanta qunki?",
            "setup_save_btn" to "WAQAYCHAY HINASPA HAYKUY",

            // Map
            "map_title" to "Watukuypa saywiti",
            "map_legend" to "Yanapakuykunapa unanchachan",
            "map_summary_title" to "Saywitipa pisichaynin",
            "map_listen_summary" to "Pisichayta uyariy",
            "map_expand" to "Hatunyachiy",

            // Tip Detail
            "tip_detail_title" to "Yachayniyuqpa yuyaychaynin",
            "tip_detail_subtitle" to "P'unchaypa yuyaychaynin",

            // Profile
            "profile_title" to "Ñuqaq qillqayniy",
            "profile_edit" to "Allichay",
            "profile_settings_title" to "App nisqapa allichaynin",
            "profile_language" to "Rimay",
            "profile_voice_speed" to "Kunkaq utqaynin",
            "profile_link_device" to "Musuq q'illayta t'inkiy",
            "profile_linked_devices" to "T'inkisqa q'illaykuna",
            "profile_logout" to "Lluqsiy",
            "profile_info_title" to "Willakuy",
            "profile_help" to "Yanapay",
            "profile_privacy" to "Willakuy amachay",
            "profile_edit_title" to "Qillqayta allichay",
            "profile_photo_change" to "Rikch'ayta t'ijray",
            "profile_business_name" to "Llank'aypa sutin",
            "profile_sector" to "Ima llank'ay",
            "profile_save_changes" to "ALLICHASQAKUNATA WAQAYCHAY",
            "profile_logout_confirm_title" to "¿Lluqsiyta munanki?",
            "profile_logout_confirm_desc" to "Kunan llamk'ay wichq'akunqa, qallariy qhawana p'unchayman kutinki. Musuqmanta t'inkikunayki tiyan rurukunata t'inkinapaq.",
            "profile_online" to "Llikapi",
            "profile_offline" to "Mana llikapi",

            // Services
            "service_lodging" to "Puñuy wasi",
            "service_food" to "Mikhuy wasi",
            "service_handicraft" to "Makipi rurasqakuna",
            "service_others" to "Tukuymiraq",

            // Visits
            "visits_title" to "Watukuykunapa qillqasqan",
            "visits_add" to "Watukuyta yapay",
            "visits_search_hint" to "Suyupi maskhay...",
            "visits_detail_title" to "Watukuypa imayna kasqan",
            "visits_date" to "P'unchay",
            "visits_country" to "Suyu",
            "visits_price" to "Chaninchasqa qullqi",
            "visits_services" to "Yanapakuykuna qusqa",
            "add_visit_title" to "Musuq watukuyta qillqay",
            "add_visit_country_label" to "Paqarisqan suyu",
            "add_visit_price_label" to "Chaninchasqa qullqi",
            "add_visit_services_label" to "Yanapakuykuna qusqa",
            "add_visit_save" to "QILLQASQATA WAQAYCHAY",
            "select_option" to "Ima kasqanta akllay",
            "select_range" to "Chaninchasqa qullqita akllay",
            "record_label" to "Qillqasqa",
            "sent_record" to "Qillqasqa apachisqa",
            "pending_record" to "Apachinapaq kachkan",
            "sent_on" to "Apachisqa p'unchay",

            // Time Ago & Categories
            "time_now" to "Kunanpacha",
            "time_min_ago" to "Ñapaq %d min",
            "time_hours_ago" to "Ñapaq %d horas",
            "time_hour_ago" to "Ñapaq 1 hora",
            "time_yesterday" to "Qayna p'unchay",
            "time_days_ago" to "Ñapaq %d p'unchaykuna",
            "cat_today" to "Kunan",
            "cat_yesterday" to "Qayna p'unchay",
            "cat_this_week" to "Kay semana",
            "cat_this_month" to "Kay killa",
            "cat_this_year" to "Kay wata",
            "cat_older" to "Watamanta aswan",
            "total_records" to "%d llapan",
            "no_records_yet" to "Manaraq kanchu qillqasqakuna",
            "tourist_label" to "Purikuq",

            // Info Screens
            "help_content" to "Kaypi yachay imayna Yupay Turismo app nisqata llamk'achinaykipaq. \n\n1. Watukuykuna qillqay: Visitas otaq Home qhawana p'unchayman riy, hinaspa + ñit'iy.\n2. Saywiti: Purikuqkuna maymanta hamusqanta kunanpacha qhaway.\n3. Perfil: App nisqapa willakuyniykikunata allichay.",
            "privacy_content" to "Willakuyniykikuna amachasqa kachkan. App nisqapi qillqasqakuna q'illayniykillapi waqaychasqa kachkan.\n\nManan hukkunaman willakuyniykikunata quykuchu. Watukuykuna willakuykunaqa yupaykunallapaq llamk'achisqa kanqa.",

            // General
            "btn_cancel" to "TATICHAY",
            "btn_confirm" to "ARÍ",
            "btn_close" to "WICHQ'AY",
            "btn_back" to "Qhipaman"
        ),
        "Inglés" to mapOf(
            // Onboarding
            "onboarding_title_0" to "Register visits without internet",
            "onboarding_desc_0" to "Keep track of your visitors anywhere, even without data or Wi-Fi connection.",
            "onboarding_btn_0" to "Start",
            "onboarding_title_1" to "See your results",
            "onboarding_desc_1" to "Understand how your business is doing with simple, easy-to-read charts.",
            "onboarding_btn_1" to "Next",
            "onboarding_title_2" to "Get advice",
            "onboarding_desc_2" to "Listen to personalized recommendations in Quechua or Spanish to improve your services.",
            "onboarding_btn_2" to "Create my profile",
            "onboarding_link_device" to "link as an additional device",
            "onboarding_perm_title" to "Network Permissions",
            "onboarding_perm_desc" to "To link this device, we need permissions to manage the WiFi connection.",
            
            // Home
            "home_greeting" to "Hello, %s!",
            "home_category" to "Category: %s",
            "home_tip_title" to "Entrepreneur Tip",
            "home_recent_visits" to "Recent Records",
            "home_tourist_country" to "Tourist / Country",
            "home_est_expense" to "Est. Expense",
            "home_no_records" to "No records available",
            "time_day" to "Day",
            "time_week" to "Week",
            "time_month" to "Month",
            "time_year" to "Year",
            "chart_today" to "Today's visits",
            "chart_week" to "Weekly visits",
            "chart_month" to "Visits per week (Month)",
            "chart_year" to "Visits per month (Year)",

            // Profile Setup
            "setup_title" to "Set up your profile",
            "setup_business_name_label" to "Store or business name",
            "setup_business_name_hint" to "Ex. Valley Handicrafts",
            "setup_service_question" to "What service do you mainly offer?",
            "setup_save_btn" to "SAVE AND ENTER",

            // Map
            "map_title" to "Visit Map",
            "map_legend" to "Service Legend",
            "map_summary_title" to "Map Summary",
            "map_listen_summary" to "Listen Summary",
            "map_expand" to "Expand",

            // Tip Detail
            "tip_detail_title" to "Entrepreneur Tip",
            "tip_detail_subtitle" to "Your daily tip",

            // Profile
            "profile_title" to "My Profile",
            "profile_edit" to "Edit",
            "profile_settings_title" to "App Settings",
            "profile_language" to "Language",
            "profile_voice_speed" to "Voice speed",
            "profile_link_device" to "Link new device",
            "profile_linked_devices" to "Linked devices",
            "profile_logout" to "Log out",
            "profile_info_title" to "Information",
            "profile_help" to "Help & Support",
            "profile_privacy" to "Privacy Policy",
            "profile_edit_title" to "Edit Profile",
            "profile_photo_change" to "Change profile picture",
            "profile_business_name" to "Business name",
            "profile_sector" to "Sector or Category",
            "profile_save_changes" to "SAVE CHANGES",
            "profile_logout_confirm_title" to "Log out?",
            "profile_logout_confirm_desc" to "The current session will be closed and the device will return to the initial screen. You will need to relink to synchronize data.",
            "profile_online" to "Online",
            "profile_offline" to "Offline",

            // Services
            "service_lodging" to "Lodging",
            "service_food" to "Food",
            "service_handicraft" to "Handicraft",
            "service_others" to "Various",

            // Visits
            "visits_title" to "Visit Records",
            "visits_add" to "Add Visit",
            "visits_search_hint" to "Search by country...",
            "visits_detail_title" to "Visit Detail",
            "visits_date" to "Date",
            "visits_country" to "Country",
            "visits_price" to "Estimated Expense",
            "visits_services" to "Services Provided",
            "add_visit_title" to "Register New Visit",
            "add_visit_country_label" to "Country of origin",
            "add_visit_price_label" to "Estimated expense",
            "add_visit_services_label" to "Services provided",
            "add_visit_save" to "SAVE RECORD",
            "select_option" to "Select option",
            "select_range" to "Select range",
            "record_label" to "Record",
            "sent_record" to "Record Sent",
            "pending_record" to "Pending Shipment",
            "sent_on" to "Sent on",

            // Time Ago & Categories
            "time_now" to "Right now",
            "time_min_ago" to "%d min ago",
            "time_hours_ago" to "%d hours ago",
            "time_hour_ago" to "1 hour ago",
            "time_yesterday" to "Yesterday",
            "time_days_ago" to "%d days ago",
            "cat_today" to "Today",
            "cat_yesterday" to "Yesterday",
            "cat_this_week" to "This week",
            "cat_this_month" to "This month",
            "cat_this_year" to "This year",
            "cat_older" to "Over a year ago",
            "total_records" to "%d total",
            "no_records_yet" to "No records yet",
            "tourist_label" to "Tourist",

            // Info Screens
            "help_content" to "Here you can find information on how to use the Yupay Turismo application. \n\n1. Visit registration: Go to the Visits or Home tab and press the + button.\n2. Map: View the origin of your visitors in real time.\n3. Profile: Configure your data and app preferences.",
            "privacy_content" to "Your privacy is important to us. The data collected in this application is stored locally on your device.\n\nWe do not share personal information with third parties without your explicit consent. Visit data is used solely to generate your statistical reports.",

            // General
            "btn_cancel" to "CANCEL",
            "btn_confirm" to "CONFIRM",
            "btn_close" to "CLOSE",
            "btn_back" to "Back"
        ),
        "Portugués" to mapOf(
            // Onboarding
            "onboarding_title_0" to "Registre visitas sem internet",
            "onboarding_desc_0" to "Mantenha o controle de seus visitantes em qualquer lugar, mesmo sem conexão de dados ou Wi-Fi.",
            "onboarding_btn_0" to "Começar",
            "onboarding_title_1" to "Veja seus resultados",
            "onboarding_desc_1" to "Entenda como está indo o seu negócio com gráficos simples e fáceis de ler.",
            "onboarding_btn_1" to "Próximo",
            "onboarding_title_2" to "Receba conselhos",
            "onboarding_desc_2" to "Ouça recomendações personalizadas em quechua ou espanhol para melhorar seus serviços.",
            "onboarding_btn_2" to "Criar meu perfil",
            "onboarding_link_device" to "vincular como dispositivo adicional",
            "onboarding_perm_title" to "Permissões de Rede",
            "onboarding_perm_desc" to "Para vincular este dispositivo, precisamos de permissões para gerenciar a conexão WiFi.",
            
            // Home
            "home_greeting" to "Olá, %s!",
            "home_category" to "Categoria: %s",
            "home_tip_title" to "Dica do Empreendedor",
            "home_recent_visits" to "Registros Recentes",
            "home_tourist_country" to "Turista / País",
            "home_est_expense" to "Gasto Est.",
            "home_no_records" to "Nenhum registro disponível",
            "time_day" to "Dia",
            "time_week" to "Sem",
            "time_month" to "Mês",
            "time_year" to "Ano",
            "chart_today" to "Visitas de hoje",
            "chart_week" to "Visitas da semana",
            "chart_month" to "Visitas por semana (Mês)",
            "chart_year" to "Visitas por mês (Ano)",

            // Profile Setup
            "setup_title" to "Configure seu perfil",
            "setup_business_name_label" to "Nome da loja ou empreendimento",
            "setup_business_name_hint" to "Ex. Artesanatos do Vale",
            "setup_service_question" to "Qual serviço você oferece principalmente?",
            "setup_save_btn" to "SALVAR E ENTRAR",

            // Map
            "map_title" to "Mapa de Visitas",
            "map_legend" to "Legenda de Serviços",
            "map_summary_title" to "Resumo do Mapa",
            "map_listen_summary" to "Ouvir Resumo",
            "map_expand" to "Expandir",

            // Tip Detail
            "tip_detail_title" to "Dica do Empreendedor",
            "tip_detail_subtitle" to "Sua dica do dia",

            // Profile
            "profile_title" to "Meu Perfil",
            "profile_edit" to "Editar",
            "profile_settings_title" to "Configurações do App",
            "profile_language" to "Idioma",
            "profile_voice_speed" to "Velocidade da voz",
            "profile_link_device" to "Vincular novo dispositivo",
            "profile_linked_devices" to "Dispositivos vinculados",
            "profile_logout" to "Sair",
            "profile_info_title" to "Informação",
            "profile_help" to "Ajuda e Suporte",
            "profile_privacy" to "Política de Privacidade",
            "profile_edit_title" to "Editar Perfil",
            "profile_photo_change" to "Alterar foto de perfil",
            "profile_business_name" to "Nome do empreendimento",
            "profile_sector" to "Setor ou Categoria",
            "profile_save_changes" to "SALVAR ALTERAÇÕES",
            "profile_logout_confirm_title" to "Sair?",
            "profile_logout_confirm_desc" to "A sessão atual será encerrada e o dispositivo voltará à tela inicial. Você precisará se vincular novamente para sincronizar os dados.",
            "profile_online" to "Online",
            "profile_offline" to "Desconectado",

            // Services
            "service_lodging" to "Hospedagem",
            "service_food" to "Alimentação",
            "service_handicraft" to "Artesanato",
            "service_others" to "Vários",

            // Visits
            "visits_title" to "Registros de Visitas",
            "visits_add" to "Adicionar Visita",
            "visits_search_hint" to "Pesquisar por país...",
            "visits_detail_title" to "Detalhe da Visita",
            "visits_date" to "Data",
            "visits_country" to "País",
            "visits_price" to "Gasto Estimado",
            "visits_services" to "Serviços Prestados",
            "add_visit_title" to "Registrar Nova Visita",
            "add_visit_country_label" to "País de origem",
            "add_visit_price_label" to "Gasto estimado",
            "add_visit_services_label" to "Serviços prestados",
            "add_visit_save" to "SALVAR REGISTRO",
            "select_option" to "Selecionar opção",
            "select_range" to "Selecionar faixa",
            "record_label" to "Registro",
            "sent_record" to "Registro Enviado",
            "pending_record" to "Pendente de Envio",
            "sent_on" to "Enviado em",

            // Time Ago & Categories
            "time_now" to "Agora mesmo",
            "time_min_ago" to "Há %d min",
            "time_hours_ago" to "Há %d horas",
            "time_hour_ago" to "Há 1 hora",
            "time_yesterday" to "Ontem",
            "time_days_ago" to "Há %d dias",
            "cat_today" to "Hoje",
            "cat_yesterday" to "Ontem",
            "cat_this_week" to "Esta semana",
            "cat_this_month" to "Este mês",
            "cat_this_year" to "Este ano",
            "cat_older" to "Há mais de um ano",
            "total_records" to "%d total",
            "no_records_yet" to "Nenhum registro ainda",
            "tourist_label" to "Turista",

            // Info Screens
            "help_content" to "Aqui você encontra informações sobre como usar o aplicativo Yupay Turismo. \n\n1. Registro de visitas: Vá para a guia Visitas ou Home e pressione o botão +.\n2. Mapa: Visualize a origem de seus visitantes em tempo real.\n3. Perfil: Configure seus dados e preferências do aplicativo.",
            "privacy_content" to "Sua privacidade é importante para nós. Os dados coletados neste aplicativo são armazenados localmente em seu dispositivo.\n\nNão compartilhamos informações pessoais com terceiros sem o seu consentimento explícito. Os dados de visitas são usados exclusivamente para gerar seus relatórios estatísticos.",

            // General
            "btn_cancel" to "CANCELAR",
            "btn_confirm" to "CONFIRMAR",
            "btn_close" to "FECHAR",
            "btn_back" to "Voltar"
        )
    )

    fun getString(key: String, language: String, vararg args: Any): String {
        val langMap = translations[language] ?: translations["Español"]!!
        val template = langMap[key] ?: key
        return try {
            template.format(*args)
        } catch (e: Exception) {
            template
        }
    }

    fun translateService(service: String, language: String): String {
        val key = when (service) {
            "Hospedaje" -> "service_lodging"
            "Alimentación" -> "service_food"
            "Artesanía" -> "service_handicraft"
            "Varios" -> "service_others"
            else -> return service
        }
        return getString(key, language)
    }

    fun translateServicesList(services: String, language: String): String {
        return services.split(", ").joinToString(", ") { translateService(it, language) }
    }
}
