import { useIntl } from 'react-intl';
import { DragDropContext, Draggable, Droppable } from '@hello-pangea/dnd';
import type {
    DraggableProvided,
    DraggableStateSnapshot,
    DropResult,
    DroppableProvided,
    ResponderProvided,
} from '@hello-pangea/dnd';
import { GripVertical, Settings2 } from 'lucide-react';
import { Link } from 'react-router';
import { gamePath } from '../../../utils/paths';
import { useCatalog } from '../../../hooks/useCatalog/useCatalog';
import type { QuestType } from '../../../../shared/quests';
import './QuestOrder.css';

interface QuestOrderProps {
    gameId: string;
    order: QuestType[];
    onChange: (order: QuestType[]) => void;
    disabled?: boolean;
}

export function QuestOrder({ gameId, order, onChange, disabled = false }: QuestOrderProps) {
    const { formatMessage } = useIntl();
    const { quests: catalog } = useCatalog();
    const handleDragEnd = ({ source, destination }: DropResult, { announce }: ResponderProvided) => {
        if (!destination || source.index === destination.index) {
            return;
        }
        const next = [...order];
        const [type] = next.splice(source.index, 1);
        next.splice(destination.index, 0, type);
        onChange(next);
        announce(
            formatMessage(
                { id: 'game.questMoved' },
                { title: catalog[type].title, position: destination.index + 1 },
            ),
        );
    };

    const renderItem = (type: QuestType, index: number) => {
        const { title } = catalog[type];
        const route = `${gamePath(gameId)}/${type}`;

        const renderRow = (provided: DraggableProvided, snapshot: DraggableStateSnapshot) => {
            return (
                <li
                    ref={provided.innerRef}
                    {...provided.draggableProps}
                    className="quest-order-row"
                    data-dragging={snapshot.isDragging || undefined}
                >
                    {provided.dragHandleProps && (
                        <span
                            className="quest-order-handle"
                            {...provided.dragHandleProps}
                            aria-label={formatMessage({ id: 'game.moveQuest' }, { title })}
                        >
                            <GripVertical size={19} aria-hidden="true" />
                        </span>
                    )}
                    <span className="step-number">{String(index + 1).padStart(2, '0')}</span>
                    <Link className="quest-order-title" to={route}>
                        {title}
                    </Link>
                    <Link
                        className="icon-button"
                        to={route}
                        aria-label={formatMessage({ id: 'game.editQuest' }, { title })}
                    >
                        <Settings2 size={17} aria-hidden="true" />
                    </Link>
                </li>
            );
        };

        return (
            <Draggable
                key={type}
                draggableId={type}
                index={index}
                isDragDisabled={disabled || order.length < 2}
            >
                {renderRow}
            </Draggable>
        );
    };

    const renderList = (provided: DroppableProvided) => {
        return (
            <ol className="quest-order" ref={provided.innerRef} {...provided.droppableProps}>
                {order.map(renderItem)}
                {provided.placeholder}
            </ol>
        );
    };

    return (
        <DragDropContext
            onDragEnd={handleDragEnd}
            dragHandleUsageInstructions={formatMessage({ id: 'game.keyboardReorder' })}
        >
            <Droppable droppableId="quest-order">{renderList}</Droppable>
        </DragDropContext>
    );
}
