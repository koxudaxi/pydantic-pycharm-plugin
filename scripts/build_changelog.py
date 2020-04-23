import re
import xml.etree.ElementTree
from enum import Enum
from html.parser import HTMLParser
from pathlib import Path
from typing import List

PROJECT_ROOT = Path(__file__).parent.parent

GITHUB_PR_URL: str = 'https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/'


class Tag(Enum):
    Version = 'h2'
    ChangeType = 'p'
    ChangeBody = 'ul'
    ChangeContent = 'li'


def get_markdown_hyperlinks(text: str) -> List[str]:
    return [
        f'[#{pr_number}]({GITHUB_PR_URL}/{pr_number})'
        for pr_number in re.findall(r'#(\d+)', text)
    ]


class HistoryHTMLParser(HTMLParser):
    def error(self, message):
        pass

    def __init__(self):
        super().__init__()
        self.current_tag: Tag = Tag.Version
        self.markdown: str = ''

    def handle_starttag(self, tag: str, attrs):
        self.current_tag = Tag(tag)

    def handle_endtag(self, tag):
        pass

    def handle_data(self, data: str):
        if not data.strip():
            return
        if self.current_tag == Tag.Version:
            self.markdown += f'## {data.replace("version ", "")}\n'
        elif self.current_tag == Tag.ChangeType:
            self.markdown += f'### {data}\n'
        elif self.current_tag == Tag.ChangeContent:
            links = get_markdown_hyperlinks(data)
            converted_data = re.sub(r'\[[^]].+\]', f'[{", ".join(links)}]', data)
            self.markdown += f'- {converted_data}\n'


def main():
    plugin_xml = PROJECT_ROOT / 'resources/META-INF/plugin.xml'
    tree = xml.etree.ElementTree.parse(str(plugin_xml))
    root = tree.getroot()
    history: str = root.find('change-notes').text

    html_parser = HistoryHTMLParser()
    html_parser.feed(history)
    with open(PROJECT_ROOT / 'docs/changelog.md', 'w') as f:
        f.write(html_parser.markdown)


if __name__ == '__main__':
    main()
