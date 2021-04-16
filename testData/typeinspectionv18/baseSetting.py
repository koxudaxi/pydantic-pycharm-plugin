from pydantic import BaseSettings


class AppSettings(BaseSettings):
    a: str
    b: str


AppSettings(
    _env_file="dev.env",
    env_file_sentinel='abc',
    _env_file_encoding='utf-8',
    _secrets_dir='xyz'
)

AppSettings()

AppSettings(<warning descr="Expected type 'str', got 'int' instead">a=1</warning>, <warning descr="Expected type 'str', got 'int' instead">b=2</warning>)