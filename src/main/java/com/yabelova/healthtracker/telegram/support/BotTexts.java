package com.yabelova.healthtracker.telegram.support;

/**
 * Все пользовательские тексты бота. Параметризованные строки содержат плейсхолдеры
 * {@code %s}; подстановка — {@link String#format(String, Object...)} по месту использования.
 */
public final class BotTexts {

    public static final String DEVELOPER_CONTACT = "@yabelova";

    // ===== REPLY-КНОПКИ (нижняя панель; также ключи маршрутизации) =====

    public static final String REPLY_BTN_NOTIFICATIONS = "🔔 Уведомления";
    public static final String REPLY_BTN_SELECT_PROFILE = "👤 Выбор профиля";
    public static final String REPLY_BTN_PROFILE_MENU = "📋 Меню профиля";
    public static final String REPLY_BTN_HELP = "ℹ️ Помощь";

    // ===== INLINE-КНОПКИ =====

    public static final String INLINE_BTN_CREATE_PROFILE = "➕ Создать новый профиль";
    public static final String INLINE_BTN_ADD_BY_CODE = "➕ Добавить по коду";
    public static final String INLINE_BTN_MENU_TAKE = "💊 Принять лекарство";
    public static final String INLINE_BTN_MENU_PLAN = "📋 План лечения";
    public static final String INLINE_BTN_MENU_SYMPTOM = "📝 Записать симптом";
    public static final String INLINE_BTN_MENU_MANAGE = "⚙️ Управление профилем";
    public static final String INLINE_BTN_MENU_SHARE = "🔗 Поделиться профилем";
    public static final String INLINE_BTN_MENU_RENAME = "✏️ Переименовать профиль";
    public static final String INLINE_BTN_MENU_REVOKE = "🚫 Отменить доступ";
    public static final String INLINE_BTN_MENU_TRANSFER = "👑 Передать владение";
    public static final String INLINE_BTN_MENU_DELETE = "🗑 Удалить профиль";
    public static final String INLINE_BTN_TRANSFER_TO = "👑 Передать → ";
    public static final String INLINE_BTN_NOTIFICATION_EDIT = "✏️ Изменить время";
    public static final String INLINE_BTN_NOTIFICATION_SET = "⏰ Ввести время";
    public static final String INLINE_BTN_NOTIFICATION_DISABLE = "🚫 Выключить уведомления";
    public static final String PROFILE_BTN_ACTIVE_PREFIX = "✓ ";
    public static final String PROFILE_BTN_PREFIX = "👤 ";

    // ===== ОБЩЕЕ =====

    public static final String COMMON_FIRST_SELECT_PROFILE = "⚠️ Сначала выберите профиль";
    public static final String COMMON_UNKNOWN_COMMAND = "⚠️ Не понимаю эту команду. Воспользуйтесь кнопками меню ниже";
    public static final String PARTICIPANT_UNKNOWN = "пользователь";
    public static final String TRANSFER_TARGET_DATIVE = "участнику";
    public static final String PRIVACY_TRANSFER_TARGET_UNKNOWN = "пользователю";

    // ===== СТАРТ / ПРИВАТНОСТЬ / ПОМОЩЬ =====

    public static final String DISCLAIMER = """
            ⚠️ Приватность и безопасность
            
            • Бот хранит только данные, которые вы вносите: профили, записи и настройки.
            • Ваши сообщения не видны другим пользователям и оператору бота.
            • Вы можете удалить все свои данные в любой момент: /delete_all_data
            """;

    public static final String HELP = """
            🤖 Семейный трекер здоровья
            
            Общий профиль: начните с /start, создайте профиль (например, на ребёнка)
            и делитесь им с партнёром по одноразовому коду.
            
            Команды и возможности:
            • /start — главное меню и выбор профиля
            • Управление профилем — переименовать, поделиться, отменить доступ,
              передать владение, удалить
            • Уведомления — настройка уведомлений
            • /delete_all_data — полное удаление всех данных аккаунта
            • Помощь, /help — этот текст
            
            Участвовать в чужом профиле можно по коду-приглашению: он действует
            72 часа и используется один раз.
            
            Технические детали и смысл данных — в ответе авторам бота.
            
            ⚠️ Приватность и безопасность
            
            • Бот хранит только данные, которые вы вносите: профили, записи и настройки.
            • Ваши сообщения не видны другим пользователям и оператору бота.
            • Вы можете удалить все свои данные в любой момент: /delete_all_data
            """;

    // ===== ЭКРАН «МЕНЮ ПРОФИЛЯ» =====

    public static final String PROFILE_MENU_TITLE = "👤 Выбранный профиль: %s\n\nВыберите действие:";

    // ===== ЭКРАН «ВЫБОР ПРОФИЛЯ» =====

    public static final String PROFILE_SELECTION_HAS = "Выберите или создайте новый\n\nВаши профили (активный отмечен ✓):";
    public static final String PROFILE_SELECTION_EMPTY = "У вас пока нет профилей. Создайте новый 👇";

    // ===== СОЗДАНИЕ ПРОФИЛЯ =====

    public static final String CREATE_PROFILE_NAME_EMPTY = "Имя профиля не может быть пустым. Введите название:";
    public static final String CREATE_PROFILE_PROMPT = "Введите имя или название нового профиля (например: 'Дочь Аня', 'Мой профиль'):";
    public static final String CREATE_PROFILE_SUCCESS = "Профиль %s успешно создан и выбран как активный ✅";

    // ===== ПРИГЛАШЕНИЯ / ПОДКЛЮЧЕНИЕ ПО КОДУ =====

    public static final String ADD_CODE_PROMPT = "Введите код приглашения:";
    public static final String ADD_PROFILE_SUCCESS = "✅ Профиль %s добавлен! Выберите его в списке, чтобы сделать активным";

    public static final String SHARE_CODE_TEXT = """
            🔗 Код приглашения для профиля %s:
            
            %s
            
            Действует 72 часа и может быть использован один раз. Второй человек вводит его в
            «👤 Выбор профиля» → «➕ Добавить по коду»
            """;

    // ===== УПРАВЛЕНИЕ ПРОФИЛЕМ =====

    public static final String RENAME_PROMPT = "Введите новое имя профиля:";
    public static final String RENAME_NAME_EMPTY = "Имя профиля не может быть пустым. Введите название:";
    public static final String RENAME_SUCCESS = "Профиль переименован в %s ✅";
    public static final String REVOKE_SUCCESS = "🚫 Доступ отозван у всех участников, неиспользованные коды удалены";

    public static final String MANAGE_MENU_HEAD = "⚙️ Управление профилем %s\n\nВыберите действие:";

    public static final String DELETE_CONFIRM = "⚠️ Удалить профиль %s? Это действие необратимо.\nЧтобы подтвердить, введите слово %s (любой другой текст отменит действие)";
    public static final String DELETE_PROMPT_PROGRESS = "Чтобы подтвердить удаление, введите слово «%s». Любой другой текст отменит действие:";
    public static final String DELETE_SUCCESS = "🗑 Профиль %s удалён";
    public static final String DELETE_CANCELLED = "🗑 Удаление отменено";
    public static final String TRANSFER_NO_PARTICIPANTS = "👑 В профиле %s нет других участников";
    public static final String TRANSFER_PROMPT = "👑 Передача владения профилем %s:\nНовый владелец сможет управлять профилем и его участниками.\n";
    public static final String TRANSFER_CONFIRM = "👑 Передать владение профилем %s участнику %s?\nЧтобы подтвердить, введите слово %s (любой другой текст отменит действие)";
    public static final String TRANSFER_SUCCESS = "👑 Владение профилем передано, вы остались участником";
    public static final String TRANSFER_CANCELLED = "👑 Передача отменена";

    // ===== УВЕДОМЛЕНИЯ =====

    public static final String NOTIFICATIONS_TIME_UNSET = "не задано";
    public static final String NOTIFICATIONS_CURRENT = "🔔 Текущее время напоминаний: %s.\n\nВыберите действие:";

    public static final String NOTIFICATIONS_ENTER_TIME = "Введите время для ежедневных уведомлений в формате Ч:ММ (например, 8:30 или 08:30):";
    public static final String NOTIFICATIONS_INVALID_FORMAT = "⚠️ Неверный формат! Введите время в формате Ч:ММ (например, 8:30 или 08:30):";

    // ===== ПРОФИЛЬ (ВЫБОР) =====

    public static final String PROFILE_UNAVAILABLE = "⚠️ Этот профиль больше недоступен. Выберите профиль заново";

    // ===== УДАЛЕНИЕ АККАУНТА =====

    public static final String DELETE_ALL_ANSWER = "✅";

    public static final String DELETE_ALL_WARNING = """
            ⚠️ Будет удалена ВСЯ информация вашего аккаунта в боте: профили, записи и настройки, участие в общих профилях.
            Это действие необратимо.
            
            Сейчас проверим ваши профили.
            """;

    public static final String DELETE_ALL_CANCELLED = """
            🗑 Удаление отменено. Ваши данные не изменены.
            
            Воспользуйтесь кнопками меню ниже ⬇️""";

    public static final String DELETE_ALL_ERROR_HEAD = "⚠️ Удаление не выполнено.";
    public static final String DELETE_ALL_ERROR_RETRY = "Попробуйте снова: %s или напишите разработчику " + DEVELOPER_CONTACT;
    public static final String DELETE_ALL_ERROR_MENU = "Если передумали, воспользуйтесь кнопками меню ниже ⬇️";
    public static final String DELETE_ALL_SUCCESS = "🗑 Все данные удалены. Отправьте %s, чтобы начать заново";
    public static final String DELETE_ALL_SUMMARY = "📋 План удаления:\n%s\n\nДля подтверждения напишите слово «%s».\nЛюбой другой текст отменит удаление, данные не изменятся";
    public static final String DELETE_ALL_PROFILE_PROMPT = "⚙️ Профиль «%s» используется с другими участниками:\n%s";
    public static final String DELETE_ALL_PROFILE_OPTIONS = "\n👑 Передать владение — участник станет владельцем, вы останетесь участником\n🚫 Отозвать доступ — участники потеряют доступ, профиль будет удалён";
    public static final String DELETION_LINE_PERSONAL = "• «%s» — будет удалён полностью";
    public static final String DELETION_LINE_MEMBER = "• «%s» — вы выйдете, записи останутся у других участников";
    public static final String DELETION_LINE_OWNER_PENDING = "• «%s» — требуется решение";
    public static final String DELETION_LINE_OWNER_TRANSFERRED = "• «%s» — владение передано «%s»";
    public static final String DELETION_LINE_OWNER_REVOKED = "• «%s» — доступ будет отозван, профиль удалён";

    private BotTexts() {
    }
}